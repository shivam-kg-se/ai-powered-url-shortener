package com.uss.models;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Entity
@Data
@Table(name = "otp_challenges", indexes = {
        @Index(name = "idx_otp_email_purpose", columnList = "email,purpose,created_at")
})
public class OtpChallenge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String email;
    @Column(nullable = false)
    private String purpose;
    @Column(nullable = false)
    private String codeHash;
    private String registrationTokenHash;
    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant expiresAt;
    @Column(nullable = false)
    private int failedAttempts;
    @Column(nullable = false)
    private boolean verified;
    private Instant consumedAt;
}
