package com.example.verifier.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

@Service
public class CredentialStatusService {

    private final ObjectMapper mapper = new ObjectMapper();

    public boolean isCredentialValid(int index, String statusListUrl) throws Exception {
        URL url = new URL(statusListUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        String jwtString;
        try (InputStream is = conn.getInputStream()) {
            JsonNode root = mapper.readTree(is);
            jwtString = root.get("jwt").asText();
        }

        SignedJWT signedJWT = SignedJWT.parse(jwtString);

        String certB64 = signedJWT.getHeader().getX509CertChain().get(0).toString();
        byte[] certBytes = Base64.getDecoder().decode(certB64);

        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        X509Certificate certificate = (X509Certificate) factory.generateCertificate(
                new java.io.ByteArrayInputStream(certBytes)
        );
        PublicKey publicKey = certificate.getPublicKey();

        JWSVerifier verifier = new ECDSAVerifier((java.security.interfaces.ECPublicKey) publicKey);
        if (!signedJWT.verify(verifier)) {
            throw new SecurityException("Invalid JWT signature");
        }

        JsonNode statusList = mapper.readTree(signedJWT.getPayload().toString()).get("status_list");

        int bits = statusList.get("bits").asInt();
        String lst = statusList.get("lst").asText();

        if (bits != 1) {
            throw new UnsupportedOperationException("Only bits=1 is currently supported");
        }

        byte[] decoded = Base64.getUrlDecoder().decode(lst);
        int byteIndex = index / 8;
        int bitOffset = index % 8;

        if (byteIndex >= decoded.length) {
            throw new IllegalArgumentException("Index out of range for status list");
        }

        int byteVal = decoded[byteIndex] & 0xFF;
        int bit = (byteVal >> (7 - bitOffset)) & 1;

        return bit == 0;
    }
}
