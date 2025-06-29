package com.example.verifier.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

@Service
public class TrustListService {

    private final JsonNode trustListRoot;

    public TrustListService() throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        File file = new File("trusted_lists/trusted_issuers.json");
        trustListRoot = mapper.readTree(file);
    }

    public boolean validateCredential(String issuerId, String credentialType, String certificatePem) {

        Iterator<JsonNode> issuers = trustListRoot.get("trust_list").elements();

        while (issuers.hasNext()) {
            JsonNode issuer = issuers.next();

            if (issuer.get("id").asText().equals(issuerId)) {

                String status = issuer.get("status").asText();
                if (!"granted".equalsIgnoreCase(status)) {
                    System.out.println("Issuer " + issuerId + " has status " + status + ", not granted.");
                    return false;
                }

                if (!issuer.has("certificate")) {
                    System.out.println("No certificate in trust list for " + issuerId);
                    return false;
                }

                String trustedCert = issuer.get("certificate").asText().replaceAll("\\s+", "");
                String providedCert = certificatePem.replaceAll("\\s+", "");

                if (!trustedCert.equals(providedCert)) {
                    System.out.println("Certificate mismatch for " + issuerId);
                    return false;
                }

                boolean typeOk = false;
                for (JsonNode type : issuer.get("credential_types_offered")) {
                    if (type.asText().equals(credentialType)) {
                        typeOk = true;
                        break;
                    }
                }

                if (!typeOk) {
                    System.out.println("Credential type not offered by issuer " + issuerId);
                    return false;
                }

                return true;
            }
        }

        System.out.println("Issuer ID not found in trust list: " + issuerId);
        return false;
    }
}
