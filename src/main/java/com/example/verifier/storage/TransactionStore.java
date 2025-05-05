package com.example.verifier.storage;

import com.example.verifier.model.TransactionRecord;
import com.example.verifier.model.TransactionStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TransactionStore {

    private final Map<String, TransactionRecord> store = new ConcurrentHashMap<>();

    public void save(TransactionRecord record) {
        store.put(record.getTransactionId(), record);
    }

    public Optional<TransactionRecord> get(String transactionId) {
        return Optional.ofNullable(store.get(transactionId));
    }

    public Optional<TransactionRecord> getByDefinitionId(String presentationDefinitionId) {
        return store.values().stream()
                .filter(r -> r.getPresentationDefinitionId().equals(presentationDefinitionId))
                .findFirst();
    }

    public void updateStatus(String transactionId, TransactionStatus status) {
        TransactionRecord record = store.get(transactionId);
        if (record != null) {
            record.setStatus(status);
        }
    }
}
