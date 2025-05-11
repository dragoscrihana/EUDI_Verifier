package com.example.verifier.controller;

import com.example.verifier.model.PresentationStoredEntry;
import com.example.verifier.service.CreateJarService;
import com.example.verifier.service.EvidenceService;
import com.example.verifier.service.JwtDecryptionService;
import com.example.verifier.service.WalletResponseValidator;
import com.example.verifier.storage.PresentationStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/wallet")
public class WalletController {

    private final PresentationStore presentationStore;
    private final CreateJarService createJarService;
    private final JwtDecryptionService jwtDecryptionService;
    private final WalletResponseValidator walletResponseValidator;
    private final EvidenceService evidenceService;

    public WalletController(PresentationStore presentationStore, CreateJarService createJarService, JwtDecryptionService jwtDecryptionService, WalletResponseValidator walletResponseValidator, EvidenceService evidenceService) {
        this.presentationStore = presentationStore;
        this.createJarService = createJarService;
        this.jwtDecryptionService = jwtDecryptionService;
        this.walletResponseValidator = walletResponseValidator;
        this.evidenceService = evidenceService;
    }

    @GetMapping("/request.jwt/{requestId}")
    public ResponseEntity<String> getRequestObject(@PathVariable String requestId) {
        PresentationStoredEntry entry = presentationStore.getByRequestId(requestId);

        if (entry == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            String jwt = createJarService.createSignedRequestObject(entry.getPresentation());
            return ResponseEntity.ok(jwt);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to create request object");
        }
    }

    @PostMapping(value = "/direct_post/{requestId}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Map<String, String>> receiveWalletResponse(
            @PathVariable String requestId,
            @RequestParam Map<String, String> formData
    ) {
        try {
            String jwe = formData.get("response");
            if (jwe == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing response parameter"));
            }

            PresentationStoredEntry storedEntry = presentationStore.getByRequestId(requestId);
            if (storedEntry == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid requestId"));
            }

            String decryptedJson = jwtDecryptionService.decryptJwt(jwe, storedEntry.getPresentation().getJarmEncryptionEphemeralKey());

            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> jsonObject = objectMapper.readValue(decryptedJson, Map.class);
            String vpToken = (String) jsonObject.get("vp_token");

            if (vpToken == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing vp_token in decrypted payload"));
            }

            Map<String, Object> presentationSubmission = (Map<String, Object>) jsonObject.get("presentation_submission");
            if (presentationSubmission == null || !presentationSubmission.containsKey("definition_id")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing presentation_submission or definition_id"));
            }

            String definitionId = presentationSubmission.get("definition_id").toString();

            walletResponseValidator.validate(vpToken, definitionId);

            Map<String, Object> walletResponse = objectMapper.readValue(decryptedJson, Map.class);
            evidenceService.logWalletResponsePosted(definitionId, Instant.now().toEpochMilli(), walletResponse);

            return ResponseEntity.ok(Map.of("message", "Wallet response received and validated successfully."));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to process wallet response"));
        }
    }


}
