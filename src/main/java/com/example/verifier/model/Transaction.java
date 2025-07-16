package com.example.verifier.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @Column(name = "transaction_id", nullable = false, unique = true)
    private String transactionId;

    @Column(name = "last_updated")
    private long lastUpdated;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    private String username;

    private String presentationDefinitionId;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Event> events = new ArrayList<>();

    public Transaction() {}

    public Transaction(String transactionId, long lastUpdated, String presentationDefinitionId, TransactionStatus status, String username) {
        this.transactionId = transactionId;
        this.lastUpdated = lastUpdated;
        this.presentationDefinitionId = presentationDefinitionId;
        this.status = status;
        this.username = username;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public String getPresentationDefinitionId() {
        return presentationDefinitionId;
    }

    public void setPresentationDefinitionId(String presentationDefinitionId) {
        this.presentationDefinitionId = presentationDefinitionId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getTransactionId() { return transactionId; }

    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public long getLastUpdated() { return lastUpdated; }

    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }

    public List<Event> getEvents() { return events; }

    public void addEvent(Event event) {
        events.add(event);
        event.setTransaction(this);
    }
}
