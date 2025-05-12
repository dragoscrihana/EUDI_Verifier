package com.example.verifier.service;

import com.example.verifier.model.Transaction;
import com.example.verifier.model.TransactionStatus;
import com.example.verifier.repository.TransactionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransactionCleanupService {

    private final TransactionRepository transactionRepository;

    public TransactionCleanupService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Scheduled(fixedRate = 60000)
    public void deleteStalePendingTransactions() {
        long twoMinutesAgo = System.currentTimeMillis() - 2 * 60 * 1000;
        List<Transaction> stale = transactionRepository.findByStatusAndLastUpdatedBefore(TransactionStatus.PENDING, twoMinutesAgo);
        transactionRepository.deleteAll(stale);
        if (!stale.isEmpty()) {
            System.out.println("Deleted " + stale.size() + " stale pending transactions.");
        }
    }
}
