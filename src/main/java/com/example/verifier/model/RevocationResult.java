package com.example.verifier.model;

// revoked = true,  fallbackNeeded = true   => blob failed, fallback to IPFS should occur
// revoked = true,  fallbackNeeded = false  => cascade valid, and credential is revoked
// revoked = false, fallbackNeeded = false  => cascade valid, and credential is NOT revoked

public class RevocationResult {
    private final boolean revoked;
    private final boolean fallbackNeeded;

    public RevocationResult(boolean revoked, boolean fallbackNeeded) {
        this.revoked = revoked;
        this.fallbackNeeded = fallbackNeeded;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public boolean isFallbackNeeded() {
        return fallbackNeeded;
    }
}
