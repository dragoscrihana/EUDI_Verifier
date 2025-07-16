package com.example.verifier.service;

import com.example.verifier.model.Event;
import com.example.verifier.model.Transaction;
import com.example.verifier.repository.TransactionRepository;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class EvidenceService {

    private final TransactionRepository transactionRepository;

    public EvidenceService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public void logTransactionInitialized(String transactionId, long timestamp, String requestUri, String preffered_username) {

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));

        Map<String, Object> payload = new HashMap<>();
        payload.put("transaction_id", transactionId);
        payload.put("client_id", "Verifier");
        payload.put("request", null);
        payload.put("request_uri", requestUri);
        payload.put("request_uri_method", "get");

        Event event = new Event(timestamp, "Transaction initialized", preffered_username, payload);
        transaction.addEvent(event);
        transactionRepository.save(transaction);
    }

    public void logRequestObjectRetrieved(String presentationDefinitionId, long timestamp, String jwt) {
        Transaction transaction = transactionRepository.findByPresentationDefinitionId(presentationDefinitionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found for presentationDefinitionId: " + presentationDefinitionId));

        Event event = new Event(
                timestamp,
                "Request object retrieved",
                "Wallet",
                Map.of("jwt", jwt)
        );

        transaction.addEvent(event);
        transaction.setLastUpdated(timestamp);

        transactionRepository.save(transaction);
    }



    public void logWalletResponsePosted(String presentationDefinitionId, long timestamp, Map<String, Object> walletResponse) {
        Event event = new Event(
                timestamp,
                "Wallet response posted",
                "Wallet",
                Map.of("wallet_response", walletResponse)
        );

        Transaction transaction = transactionRepository.findByPresentationDefinitionId(presentationDefinitionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found for presentationDefinitionId: " + presentationDefinitionId));

        transaction.addEvent(event);
        transaction.setLastUpdated(timestamp);

        transactionRepository.save(transaction);
    }




}
