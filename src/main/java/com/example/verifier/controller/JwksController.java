package com.example.verifier.controller;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class JwksController {

    private final JWK signingKey;

    public JwksController(JWK signingKey) {
        this.signingKey = signingKey;
    }

    @GetMapping("/wallet/public-keys.json")
    public Map<String, Object> getPublicKeys() {
        return new JWKSet(signingKey.toPublicJWK()).toJSONObject();
    }
}
