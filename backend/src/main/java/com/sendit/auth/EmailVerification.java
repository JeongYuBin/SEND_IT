package com.sendit.auth;

import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "email_verifications")
public class EmailVerification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 255)
    private String email;
    @Column(nullable = false, length = 30)
    private String purpose;
    @Column(name = "code_hash", nullable = false, length = 255)
    private String codeHash;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "verified_at")
    private Instant verifiedAt;
    @Column(name = "consumed_at")
    private Instant consumedAt;
    @Column(nullable = false)
    private int attempts;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected EmailVerification() {}

    EmailVerification(String email, String purpose, String codeHash, Instant expiresAt) {
        this.email = email;
        this.purpose = purpose;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
    }

    String getCodeHash() { return codeHash; }
    Instant getExpiresAt() { return expiresAt; }
    Instant getCreatedAt() { return createdAt; }
    boolean isVerified() { return verifiedAt != null; }
    boolean isConsumed() { return consumedAt != null; }
    int getAttempts() { return attempts; }
    void fail() { attempts++; }
    void verify(Instant now) { verifiedAt = now; }
    void consume(Instant now) { consumedAt = now; }
}
