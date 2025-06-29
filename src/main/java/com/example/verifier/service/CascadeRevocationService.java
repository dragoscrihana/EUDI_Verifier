package com.example.verifier.service;

import com.example.verifier.model.IpfsStatusList;
import com.example.verifier.model.RevocationResult;
import com.example.verifier.repository.IpfsStatusListRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;
import java.util.List;

@Service
public class CascadeRevocationService {

    private final ObjectMapper mapper = new ObjectMapper();
    private final IpfsStatusListRepository repository;

    public CascadeRevocationService(IpfsStatusListRepository repository) {
        this.repository = repository;
    }

    public boolean isRevokedViaIpfs(String uri, int idx, String issuerName) {
        try {
            String sanitizedIssuer = issuerName
                    .replaceFirst("^https?://", "")
                    .replaceAll("[/.]", "_");

            IpfsStatusList statusList = repository.findByIssuerName(sanitizedIssuer)
                    .orElseGet(() -> fetchAndCacheStatusList(uri, sanitizedIssuer));

            long now = System.currentTimeMillis() / 1000;

            if (statusList.getExpiresAt() <= now) {
                statusList = fetchAndCacheStatusList(uri, sanitizedIssuer);
            }

            return runCascadeCheck(statusList.getCascadeBase64(), idx);
        } catch (Exception e) {
            System.err.println("IPFS revocation check failed: " + e.getMessage());
            return true;
        }
    }

    public RevocationResult isRevokedViaBlob(String pointerHash, int idx, String issuerName) {
        try {
            String sanitizedIssuer = issuerName
                    .replaceFirst("^https?://", "")
                    .replaceAll("[/.]", "_");

            IpfsStatusList statusList = repository.findByIssuerName(sanitizedIssuer).orElse(null);
            long now = System.currentTimeMillis() / 1000;

            boolean fallbackNeeded = (statusList == null || statusList.getExpiresAt() <= now);

            if (fallbackNeeded) {

                ProcessBuilder pb = new ProcessBuilder("python", "cascade/cascade_cli.py", "check",
                        "--pointer_hash", pointerHash,
                        "--id", String.valueOf(idx));

                pb.redirectErrorStream(true);
                Process process = pb.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String jsonLine = reader.readLine();

                process.waitFor();

                if (jsonLine == null) {
                    System.err.println("No JSON response from cascade_cli.py (blob)");
                    return new RevocationResult(true, true);
                }

                JsonNode result = mapper.readTree(jsonLine);

                if (!result.has("b64_blob") || !result.has("exp")) {
                    System.err.println("Missing b64_blob or exp in CLI output");
                    return new RevocationResult(true, true);
                }

                String cascadeBase64 = result.get("b64_blob").asText();
                long exp = result.get("exp").asLong();

                if (statusList == null) {
                    statusList = new IpfsStatusList();
                    statusList.setIssuerName(sanitizedIssuer);
                }

                statusList.setCascadeBase64(cascadeBase64);
                statusList.setExpiresAt(exp);
                repository.save(statusList);

                boolean revoked = result.has("revoked") && result.get("revoked").asBoolean();
                return new RevocationResult(revoked, false);
            }

            boolean revoked = runCascadeCheck(statusList.getCascadeBase64(), idx);
            return new RevocationResult(revoked, false);

        } catch (Exception e) {
            System.err.println("Blob revocation check failed: " + e.getMessage());
            return new RevocationResult(true, true);
        }
    }

    private boolean runCascadeCheck(String cascadeBase64, int idx) {
        try {
            ProcessBuilder pb = new ProcessBuilder("python", "cascade/cascade_cli.py", "check",
                    "--cascade_data", cascadeBase64,
                    "--id", String.valueOf(idx));

            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String jsonLine = reader.readLine();
            process.waitFor();

            if (jsonLine == null) {
                System.err.println("No JSON response from cascade_cli.py (cascade_data)");
                return true;
            }

            JsonNode result = mapper.readTree(jsonLine);
            return result.has("revoked") && result.get("revoked").asBoolean();
        } catch (Exception e) {
            System.err.println("Cascade check execution failed: " + e.getMessage());
            return true;
        }
    }

    private IpfsStatusList fetchAndCacheStatusList(String uri, String sanitizedIssuer) {
        try {
            System.out.println("Cache IPFS");
            String jwtString = fetchStringFromUrl(uri);

            SignedJWT jwt = SignedJWT.parse(jwtString);
            JWSHeader header = jwt.getHeader();
            List<com.nimbusds.jose.util.Base64> x5cList = header.getX509CertChain();

            if (x5cList == null || x5cList.isEmpty()) {
                throw new IllegalArgumentException("Missing x5c certificate in JWT header");
            }

            byte[] certBytes = Base64.getDecoder().decode(x5cList.get(0).toString());
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(certBytes));
            JWSVerifier verifier = new ECDSAVerifier((ECPublicKey) cert.getPublicKey());

            if (!jwt.verify(verifier)) {
                throw new SecurityException("Invalid JWT signature");
            }

            String cascadeBase64 = jwt.getJWTClaimsSet().getStringClaim("cascade");
            long exp = jwt.getJWTClaimsSet().getExpirationTime().getTime() / 1000;

            IpfsStatusList statusList = repository.findByIssuerName(sanitizedIssuer)
                    .orElse(new IpfsStatusList());

            statusList.setIssuerName(sanitizedIssuer);
            statusList.setCascadeBase64(cascadeBase64);
            statusList.setExpiresAt(exp);

            return repository.save(statusList);
        } catch (Exception e) {
            System.err.println("Failed to fetch or verify JWT: " + e.getMessage());
            return null;
        }
    }

    private String fetchStringFromUrl(String fileUrl) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(fileUrl).openConnection();
        connection.setRequestMethod("GET");
        connection.connect();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            return sb.toString();
        } finally {
            connection.disconnect();
        }
    }
}
