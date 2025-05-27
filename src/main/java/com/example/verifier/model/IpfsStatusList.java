package com.example.verifier.model;

import jakarta.persistence.*;

@Entity
@Table(name = "ipfs_status_lists")
public class IpfsStatusList {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String url;

    @Column(nullable = false)
    private long expiresAt;

    @Column(nullable = false)
    private String binFilename;

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

    public long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getBinFilename() {
        return binFilename;
    }

    public void setBinFilename(String binFilename) {
        this.binFilename = binFilename;
    }
}
