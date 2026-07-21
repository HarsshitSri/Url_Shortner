package com.urlshortener.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "short_urls")
public class ShortUrl {

    @Id
    private UUID id;

    @Column(name = "short_code", nullable = false, unique = true, length = 16)
    private String shortCode;

    @Column(name = "original_url", nullable = false, columnDefinition = "TEXT")
    private String originalUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private UrlStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "safety_status", nullable = false, length = 32)
    private SafetyStatus safetyStatus;

    /**
     * Owning user id. Set on create for authenticated users (v3).
     */
    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ShortUrl() {
    }

    public ShortUrl(UUID id, String shortCode, String originalUrl, UrlStatus status, SafetyStatus safetyStatus) {
        this(id, shortCode, originalUrl, status, safetyStatus, null);
    }

    public ShortUrl(
            UUID id,
            String shortCode,
            String originalUrl,
            UrlStatus status,
            SafetyStatus safetyStatus,
            UUID ownerId) {
        this.id = id;
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.status = status;
        this.safetyStatus = safetyStatus;
        this.ownerId = ownerId;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getShortCode() {
        return shortCode;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public UrlStatus getStatus() {
        return status;
    }

    public void setStatus(UrlStatus status) {
        this.status = status;
    }

    public SafetyStatus getSafetyStatus() {
        return safetyStatus;
    }

    public void setSafetyStatus(SafetyStatus safetyStatus) {
        this.safetyStatus = safetyStatus;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
