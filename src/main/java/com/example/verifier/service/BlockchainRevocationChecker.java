package com.example.verifier.service;

import com.example.verifier.model.IpfsStatusList;
import com.example.verifier.model.RevocationResult;
import com.example.verifier.repository.IpfsStatusListRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;

@Service
public class BlockchainRevocationChecker {

    private final ObjectMapper mapper = new ObjectMapper();
    private final CascadeRevocationService cascadeRevocationService;
    private final IpfsStatusListRepository repository;
    private static final String CASCADE_FOLDER = "cascade";

    public BlockchainRevocationChecker(CascadeRevocationService cascadeRevocationService,
                                       IpfsStatusListRepository repository) {
        this.cascadeRevocationService = cascadeRevocationService;
        this.repository = repository;
    }

    public boolean checkRevocationViaBlockchain(String contractAddress, String abiJson, String issuerAddress, int index, String issuerName) {
        try {
            String sanitizedIssuer = issuerName
                    .replaceFirst("^https?://", "")
                    .replaceAll("[/.]", "_");

            IpfsStatusList cached = repository.findByIssuerName(sanitizedIssuer).orElse(null);
            long now = System.currentTimeMillis() / 1000;

            if (cached != null && cached.getExpiresAt() > now) {
                System.out.println("Cascade found in cache, skipping blockchain");
                return cascadeRevocationService.isRevokedViaBlob("ignored", index, issuerName).isRevoked();
            }

            Map<String, Object> crl = callPythonForCRL(contractAddress, abiJson, issuerAddress, "0");

            if (!crl.containsKey("pointerHash")) {
                System.err.println("CRL missing pointerHash for blob method.");
                return true;
            }

            String pointerHash = crl.get("pointerHash").toString();

            System.out.println("In primul pe blob");
            RevocationResult blobResult = cascadeRevocationService.isRevokedViaBlob(pointerHash, index, issuerName);

            if (blobResult.isFallbackNeeded()) {
                System.out.println("Fallback triggered, trying IPFS...");

                crl = callPythonForCRL(contractAddress, abiJson, issuerAddress, "1");

                if (!crl.containsKey("pointerHash")) {
                    System.err.println("CRL missing pointerHash for IPFS fallback.");
                    return true;
                }

                String ipfsHash = crl.get("pointerHash").toString();
                String ipfsUrl = "https://ipfs.io/ipfs/" + ipfsHash;

                return cascadeRevocationService.isRevokedViaIpfs(ipfsUrl, index, issuerName);
            }

            return blobResult.isRevoked();

        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }

    private Map<String, Object> callPythonForCRL(String contractAddress, String abiJson, String issuerAddress, String saveMethod) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                "python",
                CASCADE_FOLDER + "/dynamic_crl.py",
                contractAddress,
                abiJson,
                issuerAddress,
                saveMethod
        );

        pb.redirectErrorStream(true);
        Process process = pb.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line);
        }

        process.waitFor();
        String rawOutput = output.toString().trim();

        if (!rawOutput.startsWith("{") && !rawOutput.startsWith("[")) {
            throw new IOException("Python script did not return valid JSON. Output was:\n" + rawOutput);
        }

        return mapper.readValue(rawOutput, Map.class);
    }
}
