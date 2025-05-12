package com.example.verifier.service;

import com.example.verifier.model.TransactionStatus;
import com.example.verifier.repository.TransactionRepository;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import org.json.JSONArray;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.util.*;

import java.util.Base64;

@Service
public class WalletResponseValidator {

    private final TransactionRepository transactionRepository;
    private final CredentialStatusService credentialStatusService;

    public WalletResponseValidator(TransactionRepository transactionRepository, CredentialStatusService credentialStatusService) {
        this.transactionRepository = transactionRepository;
        this.credentialStatusService = credentialStatusService;
    }

    public void validate(String vpTokenWithDisclosures, String presentationDefinitionId) throws Exception {
        System.out.println(vpTokenWithDisclosures);
        String[] parts = vpTokenWithDisclosures.split("~");
        if (parts.length < 2) {
            throw new RuntimeException("No disclosures or KeyBinding JWT found in vp_token!");
        }

        String jwtString = parts[0];

        List<String> disclosures = Arrays.asList(Arrays.copyOfRange(parts, 1, parts.length - 1));
        String keyBindingJwt = parts[parts.length - 1];

        SignedJWT signedJWT = SignedJWT.parse(jwtString);

        JWSHeader header = signedJWT.getHeader();
        List<com.nimbusds.jose.util.Base64> x5cList = header.getX509CertChain();
        if (x5cList == null || x5cList.isEmpty()) {
            throw new RuntimeException("No x5c certificate found in JWT header!");
        }

        byte[] certBytes = x5cList.get(0).decode();
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(new java.io.ByteArrayInputStream(certBytes));
        PublicKey publicKey = certificate.getPublicKey();

        if (!(publicKey instanceof ECPublicKey)) {
            throw new RuntimeException("Public key extracted is not an EC Public Key!");
        }
        ECPublicKey ecPublicKey = (ECPublicKey) publicKey;

        JWSVerifier verifier = new ECDSAVerifier(ecPublicKey);
        if (!signedJWT.verify(verifier)) {
            throw new RuntimeException("JWT signature verification failed!");
        }

        Map<String, Object> payload = signedJWT.getJWTClaimsSet().getClaims();

        Map<String, Object> statusObject = (Map<String, Object>) payload.get("status");
        if (statusObject != null && statusObject.containsKey("status_list")) {
            Map<String, Object> statusList = (Map<String, Object>) statusObject.get("status_list");

            int index = Integer.parseInt(statusList.get("idx").toString());
            String url = statusList.get("uri").toString();

            boolean isValid = credentialStatusService.isCredentialValid(2, "http://localhost:8081/status-list");
            if (!isValid) {
                System.out.println("Credential has been revoked. Skipping further validation.");

                var maybeRecord = transactionRepository.findByPresentationDefinitionId(presentationDefinitionId);
                if (maybeRecord.isPresent()) {
                    var record = maybeRecord.get();
                    record.setStatus(TransactionStatus.DENIED);
                    transactionRepository.save(record);
                }

                return;
            }
        }

        List<String> sdHashes = (List<String>) payload.get("_sd");
        if (sdHashes == null) {
            throw new RuntimeException("_sd field missing in JWT payload!");
        }

        List<List<String>> parsedDisclosures = new ArrayList<>();
        for (String disclosureBase64 : disclosures) {
            String decodedDisclosureJson = new String(Base64.getUrlDecoder().decode(disclosureBase64), StandardCharsets.UTF_8);

            JSONArray disclosureArray = new JSONArray(decodedDisclosureJson);

            List<String> disclosureList = new ArrayList<>();
            for (int i = 0; i < disclosureArray.length(); i++) {
                Object element = disclosureArray.get(i);
                disclosureList.add(element.toString());
            }
            parsedDisclosures.add(disclosureList);
        }

        Map<String, String> claims = new HashMap<>();
        for (List<String> disclosure : parsedDisclosures) {
            String salt = disclosure.get(0);
            String claimName = disclosure.get(1);
            String claimValue = disclosure.get(2);

            String canonicalJson = "[\"" + salt + "\", \"" + claimName + "\", " + claimValue + "]";

            byte[] utf8Bytes = canonicalJson.getBytes(StandardCharsets.UTF_8);

            String base64urlDisclosure = Base64.getUrlEncoder().withoutPadding().encodeToString(utf8Bytes);

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(base64urlDisclosure.getBytes(StandardCharsets.US_ASCII));

            String base64urlHash = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);

            if (!sdHashes.contains(base64urlHash)) {
                throw new RuntimeException("Disclosure hash mismatch for claim: " + claimName);
            }

            claims.put(claimName, claimValue);
        }

        var maybeRecord = transactionRepository.findByPresentationDefinitionId(presentationDefinitionId);

        if (maybeRecord.isEmpty()) {
            throw new RuntimeException("No transaction found for definition ID: " + presentationDefinitionId);
        }
        var record = maybeRecord.get();

        System.out.println("Claims received:");
        claims.forEach((k, v) -> System.out.println(k + ": " + v));

        String ageStr = claims.get("age_in_years");
        if (ageStr == null) {
            throw new RuntimeException("age_in_years claim not found!");
        }

        int age = Integer.parseInt(claims.get("age_in_years"));

        if (age >= 18) {
            System.out.println("User is over 18 years old.");
            record.setStatus(TransactionStatus.ACCEPTED);
        } else {
            System.out.println("User is under 18 years old.");
            record.setStatus(TransactionStatus.DENIED);
        }

        transactionRepository.save(record);
    }
}
