package com.example.verifier.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

@Service
public class CredentialStatusService {

    private final ObjectMapper mapper = new ObjectMapper();

    public boolean isCredentialValid(int index, String statusListUrl) throws Exception {
        URL url = new URL(statusListUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        try (InputStream is = conn.getInputStream()) {
            JsonNode root = mapper.readTree(is);

            int bits = root.get("bits").asInt();
            String lst = root.get("lst").asText();

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
}
