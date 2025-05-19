package com.example.verifier.controller;

import io.getunleash.Unleash;
import io.getunleash.variant.Variant;
import org.json.JSONArray;
import org.json.JSONException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/ui/feature-options")
public class FeatureOptionController {

    private final Unleash unleash;

    public FeatureOptionController(Unleash unleash) {
        this.unleash = unleash;
    }

    @GetMapping("/{accountType}")
    public ResponseEntity<List<String>> getOptions(@PathVariable String accountType) {
        if (!"restaurant".equalsIgnoreCase(accountType)) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        Variant variant = unleash.getVariant("restaurant-attributes");
        System.out.println(variant);

        if (variant.getPayload().isPresent()) {
            try {
                JSONArray array = new JSONArray(variant.getPayload().get().getValue());
                List<String> attributes = new ArrayList<>();
                for (int i = 0; i < array.length(); i++) {
                    attributes.add(array.getString(i));
                }
                return ResponseEntity.ok(attributes);
            } catch (JSONException e) {
                return ResponseEntity.ok(Collections.emptyList());
            }
        }

        return ResponseEntity.ok(Collections.emptyList());
    }


}
