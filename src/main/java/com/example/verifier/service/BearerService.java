package com.example.verifier.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class BearerService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String handleUserInfo(String authHeader, Authentication authentication) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String[] parts = token.split("\\.");

            if (parts.length != 3) {
                throw new IllegalArgumentException("JWT invalid");
            }

            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
            Map<String, Object> payload = objectMapper.readValue(payloadJson, Map.class);

            String preferredUsername = (String) payload.get("preferred_username");

            return preferredUsername;

        } catch (Exception e) {
            throw new RuntimeException("Eroare la procesarea JWT-ului: " + e.getMessage(), e);
        }
    }


}
