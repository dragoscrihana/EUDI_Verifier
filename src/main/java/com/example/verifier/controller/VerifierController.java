package com.example.verifier.controller;

import com.example.verifier.dto.InitTransactionResponse;
import com.example.verifier.dto.InitTransactionTO;
import com.example.verifier.service.VerifierService;
import com.example.verifier.storage.TransactionStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ui/presentations")
public class VerifierController {

    private final VerifierService verifierService;
    private final TransactionStore transactionStore;

    public VerifierController(VerifierService verifierService, TransactionStore transactionStore) {
        this.verifierService = verifierService;
        this.transactionStore = transactionStore;
    }

    @PostMapping
    public ResponseEntity<InitTransactionResponse> initPresentation(@RequestBody InitTransactionTO input) {
        InitTransactionResponse response = verifierService.handleInitTransaction(input);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{transactionId}/status")
    public ResponseEntity<String> getStatus(@PathVariable String transactionId) {
        return transactionStore.get(transactionId)
                .map(record -> ResponseEntity.ok(record.getStatus().name()))
                .orElse(ResponseEntity.notFound().build());
    }
}
