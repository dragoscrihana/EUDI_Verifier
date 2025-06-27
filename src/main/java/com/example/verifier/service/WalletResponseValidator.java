package com.example.verifier.service;

import com.example.verifier.model.TransactionStatus;
import com.example.verifier.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.LocalDate;
import java.time.Period;
import java.util.*;

import java.util.Base64;

@Service
public class WalletResponseValidator {

    private final TransactionRepository transactionRepository;
    private final StatusListService statusListService;
    private final CascadeRevocationService cascadeRevocationService;
    private final BlockchainRevocationChecker blockchainRevocationChecker;

    public WalletResponseValidator(TransactionRepository transactionRepository, StatusListService statusListService, CascadeRevocationService cascadeRevocationService, BlockchainRevocationChecker blockchainRevocationChecker) {
        this.transactionRepository = transactionRepository;
        this.statusListService = statusListService;
        this.cascadeRevocationService = cascadeRevocationService;
        this.blockchainRevocationChecker = blockchainRevocationChecker;
    }

    public void validate(String vpTokenWithDisclosures, String presentationDefinitionId) throws Exception {
        System.out.println(vpTokenWithDisclosures);
        String[] parts = vpTokenWithDisclosures.split("~");
        if (parts.length < 2) {
            throw new RuntimeException("No disclosures or KeyBinding JWT found in vp_token!");
        }

        String jwtString = parts[0];

        List<String> disclosures = Arrays.asList(Arrays.copyOfRange(parts, 1, parts.length));

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
        String issuer = (String) payload.get("iss");

        //long ipfsStart = System.nanoTime();
        // Blockchain revocation check
        if (statusObject != null && statusObject.containsKey("blockchain_list")) {
            Map<String, Object> blockchainList = (Map<String, Object>) statusObject.get("blockchain_list");

            ObjectMapper mapper = new ObjectMapper();

            String contractAddress = blockchainList.get("contract_address").toString();

            String abiJson = mapper.writeValueAsString(blockchainList.get("abi"));
            String abiBase64 = Base64.getEncoder().encodeToString(abiJson.getBytes(StandardCharsets.UTF_8));

            String issuerAddress = blockchainList.get("issuer_address").toString();
            int index = Integer.parseInt(blockchainList.get("idx").toString());

            boolean revoked = blockchainRevocationChecker.checkRevocationViaBlockchain(
                    contractAddress,
                    abiBase64,
                    issuerAddress,
                    index,
                    issuer
            );

            if (revoked) {
                System.out.println("Credential revoked via blockchain+IPFS.");

                var maybeRecord = transactionRepository.findByPresentationDefinitionId(presentationDefinitionId);
                if (maybeRecord.isPresent()) {
                    var record = maybeRecord.get();
                    record.setStatus(TransactionStatus.DENIED);
                    transactionRepository.save(record);
                }

                return;
            } else {
                System.out.println("Credential not revoked via blockchain+IPFS.");
            }
        }


//        long ipfsEnd = System.nanoTime();
//        System.out.println("IPFS time: " + (ipfsEnd - ipfsStart) / 1_000_000 + " ms");
//
//        long listStart = System.nanoTime();
//        if (statusObject != null && statusObject.containsKey("status_list")) {
//            Map<String, Object> statusList = (Map<String, Object>) statusObject.get("status_list");
//
//            int index = Integer.parseInt(statusList.get("idx").toString());
//            String url = statusList.get("uri").toString();
//
//            boolean isValid = statusListService.isCredentialValid(index, url);
//            //boolean isValid = true;
//            if (!isValid) {
//                System.out.println("Credential has been revoked. Skipping further validation.");
//
//                var maybeRecord = transactionRepository.findByPresentationDefinitionId(presentationDefinitionId);
//                if (maybeRecord.isPresent()) {
//                    var record = maybeRecord.get();
//                    record.setStatus(TransactionStatus.DENIED);
//                    transactionRepository.save(record);
//                }
//
//                return;
//            } else {
//                System.out.println("Valid credential!");
//            }
//        }
//
//        long listEnd = System.nanoTime();
//        System.out.println("Status list time: " + (listEnd - listStart) / 1_000_000 + " ms");


        List<String> sdHashes = (List<String>) payload.get("_sd");
        if (sdHashes == null) {
            throw new RuntimeException("_sd field missing in JWT payload!");
        }

        if (disclosures.size() != 1) {
            throw new RuntimeException("Expected exactly one disclosure!");
        }

        String disclosureBase64 = disclosures.get(0);
        String decodedDisclosureJson = new String(Base64.getUrlDecoder().decode(disclosureBase64), StandardCharsets.UTF_8);
        JSONArray disclosureArray = new JSONArray(decodedDisclosureJson);

        List<String> disclosure = new ArrayList<>();
        for (int i = 0; i < disclosureArray.length(); i++) {
            disclosure.add(disclosureArray.get(i).toString());
        }

        boolean isOver18 = validateDisclosureAndCheckAge(disclosure, sdHashes);

        var maybeRecord = transactionRepository.findByPresentationDefinitionId(presentationDefinitionId);

        if (maybeRecord.isEmpty()) {
            throw new RuntimeException("No transaction found for definition ID: " + presentationDefinitionId);
        }
        var record = maybeRecord.get();

        if (isOver18) {
            System.out.println("User is over 18 years old.");
            record.setStatus(TransactionStatus.ACCEPTED);
        } else {
            System.out.println("User is under 18 years old.");
            record.setStatus(TransactionStatus.DENIED);
        }

        transactionRepository.save(record);
    }

    private boolean validateDisclosureAndCheckAge(List<String> disclosure, List<String> sdHashes) throws Exception {
        if (disclosure.size() != 3) {
            throw new IllegalArgumentException("Disclosure must contain exactly 3 elements");
        }

        String salt = disclosure.get(0);
        String claimName = disclosure.get(1);
        String claimValueRaw = disclosure.get(2);

        String canonicalValue;
        if ("true".equals(claimValueRaw) || "false".equals(claimValueRaw) || isNumeric(claimValueRaw)) {
            canonicalValue = claimValueRaw;
        } else {
            canonicalValue = "\"" + claimValueRaw + "\"";
        }

        String canonicalJson = "[\"" + salt + "\",\"" + claimName + "\"," + canonicalValue + "]";
        byte[] utf8Bytes = canonicalJson.getBytes(StandardCharsets.UTF_8);
        String base64urlDisclosure = Base64.getUrlEncoder().withoutPadding().encodeToString(utf8Bytes);

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(base64urlDisclosure.getBytes(StandardCharsets.US_ASCII));
        String base64urlHash = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);

        if (!sdHashes.contains(base64urlHash)) {
            throw new RuntimeException("Disclosure hash mismatch for claim: " + claimName);
        }

        switch (claimName) {
            case "is_student":
                return Boolean.parseBoolean(claimValueRaw);
            case "is_over_18":
                return Boolean.parseBoolean(claimValueRaw);
            case "age_in_years":
                return Integer.parseInt(claimValueRaw) >= 18;
            case "birthdate":
                LocalDate birthdate = LocalDate.parse(claimValueRaw);
                LocalDate today = LocalDate.now();
                Period age = Period.between(birthdate, today);
                return age.getYears() >= 18;
            default:
                throw new RuntimeException("Unknown claim name: " + claimName);
        }
    }

    private boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
