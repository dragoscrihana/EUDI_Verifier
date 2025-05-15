package com.example.verifier.service;

import io.getunleash.Unleash;
import io.getunleash.UnleashContext;
import io.getunleash.variant.Variant;
import org.springframework.stereotype.Service;

@Service
public class FeatureFlagService {

    private final Unleash unleash;

    public FeatureFlagService(Unleash unleash) {
        this.unleash = unleash;
    }

    public String resolvePresentationJson(String accountType) {
        String flagName = accountType + "-pd-version";

        UnleashContext context = UnleashContext.builder()
                .build();

        Variant variant = unleash.getVariant(flagName, context);

        return variant.getPayload()
                .map(payload -> payload.getValue())
                .orElse("default.json");
    }
}
