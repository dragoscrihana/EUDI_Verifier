package com.example.verifier.dto;

public class InitTransactionResponse {
    private final String transactionId;
    private final String clientId;
    private final String requestUri;

    public InitTransactionResponse(String transactionId, String clientId, String requestUri) {
        this.transactionId = transactionId;
        this.clientId = clientId;
        this.requestUri = requestUri;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getClientId() {
        return clientId;
    }

    public String getRequestUri() {
        return requestUri;
    }
}
