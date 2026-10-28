package com.uss.service;

import com.uss.bindings.CompleteRegistrationRequest;
import com.uss.bindings.VerifyOtpRequest;
import com.uss.models.OtpChallenge;
import com.uss.models.User;
import com.uss.repository.OtpChallengeRepo;
import com.uss.repository.UserRepo;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Base64;

@Service
public class RegistrationService {
    private static final String REGISTRATION = "REGISTRATION";
    private static final String EMAIL_CHANGE = "EMAIL_CHANGE";
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private final UserRepo userRepo;
    private final OtpChallengeRepo challengeRepo;
    private final PasswordEncoder passwordEncoder;
    private final OtpEmailService otpEmailService;
    private final SecureRandom secureRandom = new SecureRandom();

    public RegistrationService(UserRepo userRepo, OtpChallengeRepo challengeRepo,
                               PasswordEncoder passwordEncoder, OtpEmailService otpEmailService) {
        this.userRepo = userRepo;
        this.challengeRepo = challengeRepo;
        this.passwordEncoder = passwordEncoder;
        this.otpEmailService = otpEmailService;
    }

    @Transactional
    public void sendRegistrationOtp(String suppliedEmail) {
        String email = normalizeEmail(suppliedEmail);
        if (userRepo.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email is already registered");
        }
        createAndSend(email, REGISTRATION);
    }

    @Transactional
    public String verifyRegistrationOtp(VerifyOtpRequest request) {
        String email = normalizeEmail(request.email());
        OtpChallenge challenge = validChallenge(email, REGISTRATION);
        if (challenge == null || challenge.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
            return null;
        }
        if (!passwordEncoder.matches(request.otp(), challenge.getCodeHash())) {
            challenge.setFailedAttempts(challenge.getFailedAttempts() + 1);
            challengeRepo.save(challenge);
            return null;
        }
        String registrationToken = createRegistrationToken();
        challenge.setVerified(true);
        challenge.setRegistrationTokenHash(passwordEncoder.encode(registrationToken));
        challengeRepo.save(challenge);
        return registrationToken;
    }

    @Transactional
    public User completeRegistration(CompleteRegistrationRequest request) {
        String email = normalizeEmail(request.email());
        OtpChallenge challenge = validChallenge(email, REGISTRATION);
        if (challenge == null || !challenge.isVerified() || challenge.getRegistrationTokenHash() == null
                || !passwordEncoder.matches(request.registrationToken(), challenge.getRegistrationTokenHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A valid registration token from email verification is required");
        }
        if (userRepo.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email is already registered");
        }
        if (userRepo.existsByUsernameIgnoreCase(request.username())) {
            throw new ApiException(HttpStatus.CONFLICT, "Username is already taken");
        }
        User user = new User();
        user.setEmail(email);
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole("ROLE_USER");
        User saved = userRepo.save(user);
        challenge.setConsumedAt(Instant.now());
        challengeRepo.save(challenge);
        return saved;
    }

    @Transactional
    public void sendEmailChangeOtp(User user, String suppliedEmail) {
        String email = normalizeEmail(suppliedEmail);
        if (email.equalsIgnoreCase(user.getEmail())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "New email must differ from the current email");
        }
        if (userRepo.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email is already in use");
        }
        createAndSend(email, EMAIL_CHANGE);
    }

    @Transactional
    public User verifyEmailChangeOtp(User user, VerifyOtpRequest request) {
        String email = normalizeEmail(request.email());
        if (!verify(email, EMAIL_CHANGE, request.otp())) {
            return null;
        }
        if (userRepo.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email is already in use");
        }
        OtpChallenge challenge = latestActive(email, EMAIL_CHANGE);
        user.setEmail(email);
        challenge.setConsumedAt(Instant.now());
        userRepo.save(user);
        challengeRepo.save(challenge);
        return user;
    }

    private void createAndSend(String email, String purpose) {
        String otp = String.format("%06d", secureRandom.nextInt(1_000_000));
        Instant now = Instant.now();
        OtpChallenge challenge = new OtpChallenge();
        challenge.setEmail(email);
        challenge.setPurpose(purpose);
        challenge.setCodeHash(passwordEncoder.encode(otp));
        challenge.setCreatedAt(now);
        challenge.setExpiresAt(now.plus(10, ChronoUnit.MINUTES));
        challenge.setFailedAttempts(0);
        challengeRepo.save(challenge);
        otpEmailService.send(email, otp, purpose.toLowerCase(Locale.ROOT).replace('_', ' '));
    }

    private boolean verify(String email, String purpose, String otp) {
        OtpChallenge challenge = validChallenge(email, purpose);
        if (challenge == null || challenge.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
            return false;
        }
        if (!passwordEncoder.matches(otp, challenge.getCodeHash())) {
            challenge.setFailedAttempts(challenge.getFailedAttempts() + 1);
            challengeRepo.save(challenge);
            return false;
        }
        challenge.setVerified(true);
        challengeRepo.save(challenge);
        return true;
    }

    private OtpChallenge latestActive(String email, String purpose) {
        return challengeRepo.findTopByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(email, purpose)
                .orElse(null);
    }

    private OtpChallenge validChallenge(String email, String purpose) {
        OtpChallenge challenge = latestActive(email, purpose);
        if (challenge == null || challenge.getExpiresAt().isBefore(Instant.now())
                || challenge.getConsumedAt() != null) {
            return null;
        }
        return challenge;
    }

    private String createRegistrationToken() {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
