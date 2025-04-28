package com.example.verifier.controller;

import com.example.verifier.dto.InitTransactionResponse;
import com.example.verifier.dto.InitTransactionTO;
import com.example.verifier.service.VerifierService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ui/presentations")
public class VerifierController {

    private final VerifierService verifierService;

    public VerifierController(VerifierService verifierService) {
        this.verifierService = verifierService;
    }

    @PostMapping
    public ResponseEntity<InitTransactionResponse> initPresentation(@RequestBody InitTransactionTO input) {
        InitTransactionResponse response = verifierService.handleInitTransaction(input);
        return ResponseEntity.ok(response);
    }
}
