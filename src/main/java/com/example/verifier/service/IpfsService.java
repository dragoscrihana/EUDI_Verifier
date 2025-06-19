package com.example.verifier.service;

import com.example.verifier.model.IpfsStatusList;
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
public class IpfsService {

    private final ObjectMapper mapper = new ObjectMapper();
    private final IpfsStatusListRepository repository;

    public IpfsService(IpfsStatusListRepository repository) {
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

            ProcessBuilder pb = new ProcessBuilder("python", "cascade/cascade_cli.py", "check",
                    "--cascade_data", statusList.getCascadeBase64(),
                    "--id", String.valueOf(idx));

            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            reader.readLine();
            String jsonLine = reader.readLine();

            process.waitFor();

            if (jsonLine == null) {
                throw new RuntimeException("No JSON response from cascade_cli.py");
            }

            JsonNode result = mapper.readTree(jsonLine);

            if (result.has("exp")) {
                long exp = result.get("exp").asLong();
                if (exp > statusList.getExpiresAt()) {
                    statusList.setExpiresAt(exp);
                    repository.save(statusList);
                }
            }

            return result.has("revoked") && result.get("revoked").asBoolean();

        } catch (Exception e) {
            throw new RuntimeException("IPFS revocation check failed", e);
        }
    }

    private IpfsStatusList fetchAndCacheStatusList(String uri, String sanitizedIssuer) {
        try {
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
            throw new RuntimeException("Failed to fetch or verify JWT from IPFS", e);
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
