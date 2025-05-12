package com.example.verifier.repository;

import com.example.verifier.model.Transaction;
import com.example.verifier.model.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, String> {
    Optional<Transaction> findByTransactionId(String transactionId);
    Optional<Transaction> findByPresentationDefinitionId(String presentationDefinitionId);
    List<Transaction> findTop10ByOrderByLastUpdatedDesc();
    List<Transaction> findByStatusAndLastUpdatedBefore(TransactionStatus status, long timestamp);
}
