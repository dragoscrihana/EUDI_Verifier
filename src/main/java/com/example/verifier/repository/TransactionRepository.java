package com.example.verifier.repository;

import com.example.verifier.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, String> {
    Optional<Transaction> findByTransactionId(String transactionId);
    Optional<Transaction> findByPresentationDefinitionId(String presentationDefinitionId);
}
