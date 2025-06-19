package com.example.verifier.model;

import jakarta.persistence.*;

@Entity
@Table(name = "ipfs_status_lists")
public class IpfsStatusList {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "issuer_name", nullable = false, unique = true)
    private String issuerName;

    @Column(nullable = false)
    private long expiresAt;

    @Column(name = "cascade_base64", columnDefinition = "TEXT", nullable = false)
    private String cascadeBase64;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIssuerName() {
        return issuerName;
    }

    public void setIssuerName(String issuerName) {
        this.issuerName = issuerName;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getCascadeBase64() {
        return cascadeBase64;
    }

    public void setCascadeBase64(String cascadeBase64) {
        this.cascadeBase64 = cascadeBase64;
    }
}
