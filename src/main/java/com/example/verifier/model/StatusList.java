package com.example.verifier.model;

import jakarta.persistence.*;

@Entity
@Table(name = "status_lists")
public class StatusList {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String url;

    @Column(nullable = false)
    private int bits;

    @Lob
    @Column(nullable = false)
    private byte[] decoded;

    @Column(name = "expires_at", nullable = false)
    private long expiresAt;

    @Column(name = "fetched_at", nullable = false)
    private long fetchedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getBits() {
        return bits;
    }

    public void setBits(int bits) {
        this.bits = bits;
    }

    public byte[] getDecoded() {
        return decoded;
    }

    public void setDecoded(byte[] decoded) {
        this.decoded = decoded;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public long getFetchedAt() {
        return fetchedAt;
    }

    public void setFetchedAt(long fetchedAt) {
        this.fetchedAt = fetchedAt;
    }
}
