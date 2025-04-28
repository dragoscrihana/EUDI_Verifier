package com.example.verifier.util;

import com.nimbusds.oauth2.sdk.id.Identifier;

public class IdGenerator {

    private static final int DEFAULT_BYTE_LENGTH = 64;

    public IdGenerator() {
    }

    public static String generateTransactionId() {
        return new Identifier(DEFAULT_BYTE_LENGTH).getValue();
    }

    public static String generateRequestId() {
        return new Identifier(DEFAULT_BYTE_LENGTH).getValue();
    }
}
