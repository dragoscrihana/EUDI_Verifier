package com.example.verifier.model;

public class TransactionRecord {
    private final String transactionId;
    private final String presentationDefinitionId;
    private TransactionStatus status;

    public TransactionRecord(String transactionId, String presentationDefinitionId) {
        this.transactionId = transactionId;
        this.presentationDefinitionId = presentationDefinitionId;
        this.status = TransactionStatus.PENDING;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getPresentationDefinitionId() {
        return presentationDefinitionId;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }
}
