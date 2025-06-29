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
    private final TrustListService trustListService;

    public WalletResponseValidator(TransactionRepository transactionRepository, StatusListService statusListService, CascadeRevocationService cascadeRevocationService, BlockchainRevocationChecker blockchainRevocationChecker, TrustListService trustListService) {
        this.transactionRepository = transactionRepository;
        this.statusListService = statusListService;
        this.cascadeRevocationService = cascadeRevocationService;
        this.blockchainRevocationChecker = blockchainRevocationChecker;
        this.trustListService = trustListService;
    }

    public void validate(String vpTokenWithDisclosures, String presentationDefinitionId) {
        try {
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

            String issuerId = (String) payload.get("iss");
            String credentialType = (String) payload.get("vct");

            if (issuerId == null || credentialType == null) {
                throw new RuntimeException("Missing issuer or credential type in payload!");
            }

            String certBase64 = String.valueOf(x5cList.get(0));
            String certPem = "-----BEGIN CERTIFICATE-----\n" +
                    certBase64.replaceAll("(.{64})", "$1\n") +
                    "\n-----END CERTIFICATE-----";

            boolean issuerValid = trustListService.validateCredential(issuerId, credentialType, certPem);

            if (!issuerValid) {
                throw new RuntimeException("Issuer validation failed. Credential is not trusted!");
            }

            System.out.println("Issuer validated successfully against trust list.");

            Map<String, Object> statusObject = (Map<String, Object>) payload.get("status");
            String issuer = (String) payload.get("iss");

//            if (statusObject != null && statusObject.containsKey("blockchain_list")) {
//                Map<String, Object> blockchainList = (Map<String, Object>) statusObject.get("blockchain_list");
//
//                ObjectMapper mapper = new ObjectMapper();
//
//                String contractAddress = blockchainList.get("contract_address").toString();
//                String abiJson = mapper.writeValueAsString(blockchainList.get("abi"));
//                String abiBase64 = Base64.getEncoder().encodeToString(abiJson.getBytes(StandardCharsets.UTF_8));
//                String issuerAddress = blockchainList.get("issuer_address").toString();
//                int index = Integer.parseInt(blockchainList.get("idx").toString());
//
//                boolean revoked = blockchainRevocationChecker.checkRevocationViaBlockchain(
//                        contractAddress,
//                        abiBase64,
//                        issuerAddress,
//                        index,
//                        issuer
//                );
//
//                if (revoked) {
//                    System.out.println("Credential revoked via blockchain+IPFS.");
//
//                    updateTransactionStatus(presentationDefinitionId, TransactionStatus.DENIED);
//                    return;
//                } else {
//                    System.out.println("Credential not revoked via blockchain+IPFS.");
//                }
//            }
//
//            if (statusObject != null && statusObject.containsKey("status_list")) {
//                Map<String, Object> statusList = (Map<String, Object>) statusObject.get("status_list");
//
//                int index = Integer.parseInt(statusList.get("idx").toString());
//                String url = statusList.get("uri").toString();
//
//                boolean isValid = statusListService.isCredentialValid(index, url);
//                if (!isValid) {
//                    System.out.println("Credential has been revoked. Skipping further validation.");
//
//                    updateTransactionStatus(presentationDefinitionId, TransactionStatus.DENIED);
//                    return;
//                } else {
//                    System.out.println("Valid credential!");
//                }
//            }

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

        } catch (Exception e) {
            System.err.println("Exception during VP validation: " + e.getMessage());
            e.printStackTrace();

            updateTransactionStatus(presentationDefinitionId, TransactionStatus.DENIED);
        }
    }

    private void updateTransactionStatus(String presentationDefinitionId, TransactionStatus status) {
        var maybeRecord = transactionRepository.findByPresentationDefinitionId(presentationDefinitionId);
        if (maybeRecord.isPresent()) {
            var record = maybeRecord.get();
            record.setStatus(status);
            transactionRepository.save(record);
            System.err.println("Transaction status set to " + status + " due to validation error.");
        } else {
            System.err.println("No transaction found for definition ID: " + presentationDefinitionId + " when trying to set status to " + status + ".");
        }
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
