package com.example.verifier.controller;

import com.example.verifier.dto.InitTransactionResponse;
import com.example.verifier.dto.InitTransactionTO;
import com.example.verifier.dto.PresentationDefinitionTO;
import com.example.verifier.model.Event;
import com.example.verifier.model.Transaction;
import com.example.verifier.repository.TransactionRepository;
import com.example.verifier.service.DefinitionService;
import com.example.verifier.service.FeatureFlagService;
import com.example.verifier.service.VerifierService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ui/presentations")
public class VerifierController {

    private final VerifierService verifierService;
    private final TransactionRepository transactionRepository;
    private final DefinitionService definitionService;
    private final FeatureFlagService featureFlagService;

    public VerifierController(VerifierService verifierService, TransactionRepository transactionRepository, DefinitionService definitionService, FeatureFlagService featureFlagService) {
        this.verifierService = verifierService;
        this.transactionRepository = transactionRepository;
        this.definitionService = definitionService;
        this.featureFlagService = featureFlagService;
    }
    @PostMapping
    public ResponseEntity<InitTransactionResponse> initPresentation(@RequestBody InitTransactionTO input) {
        InitTransactionResponse response = verifierService.handleInitTransaction(input);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{accountType}")
    public ResponseEntity<InitTransactionResponse> initPresentationByName(
            @PathVariable String accountType,
            @RequestBody InitTransactionTO input
    ) {
        String jsonName = featureFlagService.resolvePresentationJson(accountType);

        PresentationDefinitionTO definition = definitionService.loadDefinition(jsonName);
        input.setPresentationDefinition(definition);

        InitTransactionResponse response = verifierService.handleInitTransaction(input);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{transactionId}/status")
    public ResponseEntity<String> getStatus(@PathVariable String transactionId) {
        return transactionRepository.findByTransactionId(transactionId)
                .map(transaction -> ResponseEntity.ok(transaction.getStatus().name()))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{transactionId}/events")
    public ResponseEntity<Transaction> getTransaction(@PathVariable String transactionId) {
        return transactionRepository.findById(transactionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/recent")
    public ResponseEntity<List<Transaction>> getRecentTransactions() {
        List<Transaction> recent = transactionRepository.findTop10ByOrderByLastUpdatedDesc();
        return ResponseEntity.ok(recent);
    }

}
