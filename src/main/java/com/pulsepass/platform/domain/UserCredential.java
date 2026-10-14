package com.pulsepass.platform.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Credencial 1:1 con User
 */
@Entity
@Table(name = "user_credentials")
public class UserCredential {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "password_changed_at", nullable = false)
    private LocalDateTime passwordChangedAt;

    protected UserCredential() {
    }

    public UserCredential(Long userId, String passwordHash, LocalDateTime passwordChangedAt) {
        this.userId = userId;
        this.passwordHash = passwordHash;
        this.passwordChangedAt = passwordChangedAt;
    }

    public Long getUserId() { return userId; }
    public String getPasswordHash() { return passwordHash; }
    public LocalDateTime getPasswordChangedAt() { return passwordChangedAt; }
}
