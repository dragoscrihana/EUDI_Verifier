package com.example.verifier.service;

import com.example.verifier.model.StatusList;
import com.example.verifier.repository.StatusListRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Base64;

@Service
public class StatusListService {

    private final ObjectMapper mapper = new ObjectMapper();
    private final StatusListRepository repository;

    public StatusListService(StatusListRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public boolean isCredentialValid(int index, String statusListUrl) throws Exception {

        long now = Instant.now().getEpochSecond();

        StatusList list = repository.findByUrl(statusListUrl)
                .filter(cached -> cached.getExpiresAt() > now)
                .orElseGet(() -> fetchAndCacheStatusList(statusListUrl));

        if (list.getBits() != 1) {
            throw new UnsupportedOperationException("Only bits=1 is supported");
        }

        byte[] decoded = list.getDecoded();

        int byteIndex = index / 8;
        int bitOffset = index % 8;

        if (byteIndex >= decoded.length) {
            throw new IllegalArgumentException("Index out of range for status list");
        }

        int byteVal = decoded[byteIndex] & 0xFF;
        int bit = (byteVal >> bitOffset) & 1;

        return bit == 0;
    }

    private byte[] decompress(byte[] compressed) throws Exception {
        java.util.zip.Inflater inflater = new java.util.zip.Inflater();
        inflater.setInput(compressed);

        byte[] buffer = new byte[200000];
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        while (!inflater.finished()) {
            int count = inflater.inflate(buffer);
            outputStream.write(buffer, 0, count);
        }

        inflater.end();
        return outputStream.toByteArray();
    }


    private StatusList fetchAndCacheStatusList(String statusListUrl) {
        try {
            URL url = new URL(statusListUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            String jwtString;
            try (InputStream is = conn.getInputStream()) {
                JsonNode root = mapper.readTree(is);
                jwtString = root.get("jwt").asText();
            }

            SignedJWT jwt = SignedJWT.parse(jwtString);

            String certB64 = jwt.getHeader().getX509CertChain().get(0).toString();
            byte[] certBytes = Base64.getDecoder().decode(certB64);
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            X509Certificate certificate = (X509Certificate) factory.generateCertificate(
                    new ByteArrayInputStream(certBytes)
            );
            PublicKey publicKey = certificate.getPublicKey();

            JWSVerifier verifier = new ECDSAVerifier((java.security.interfaces.ECPublicKey) publicKey);
            if (!jwt.verify(verifier)) {
                throw new SecurityException("Invalid JWT signature");
            }

            JsonNode payload = mapper.readTree(jwt.getPayload().toString());
            JsonNode statusNode = payload.get("status_list");

            int bits = statusNode.get("bits").asInt();
            String lst = statusNode.get("lst").asText();
            long expiresAt = payload.get("exp").asLong();
            long fetchedAt = Instant.now().getEpochSecond();

            byte[] compressed = Base64.getDecoder().decode(lst);
            byte[] decoded = decompress(compressed);

            StatusList list = repository.findByUrl(statusListUrl)
                    .orElse(new StatusList());
            list.setUrl(statusListUrl);
            list.setBits(bits);
            list.setDecoded(decoded);
            list.setExpiresAt(expiresAt);
            list.setFetchedAt(fetchedAt);

            return repository.save(list);

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch or cache status list", e);
        }
    }
}
