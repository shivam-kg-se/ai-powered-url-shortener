package com.uss.repository;

import com.uss.models.OtpChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface OtpChallengeRepo extends JpaRepository<OtpChallenge, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OtpChallenge> findTopByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
            String email, String purpose);
}
