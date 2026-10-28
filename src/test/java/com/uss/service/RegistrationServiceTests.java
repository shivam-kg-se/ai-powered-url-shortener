package com.uss.service;

import com.uss.bindings.CompleteRegistrationRequest;
import com.uss.bindings.VerifyOtpRequest;
import com.uss.models.OtpChallenge;
import com.uss.models.User;
import com.uss.repository.OtpChallengeRepo;
import com.uss.repository.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTests {
    private final AtomicReference<String> challengeToken = new AtomicReference<>();
    @Mock
    private UserRepo userRepo;
    @Mock
    private OtpChallengeRepo challengeRepo;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private OtpEmailService otpEmailService;

    private RegistrationService registrationService;
    private OtpChallenge challenge;

    @BeforeEach
    void setUp() {
        registrationService = new RegistrationService(userRepo, challengeRepo, passwordEncoder, otpEmailService);
        challenge = new OtpChallenge();
        challenge.setEmail("person@example.com");
        challenge.setPurpose("REGISTRATION");
        challenge.setCodeHash("otp-hash");
        challenge.setCreatedAt(Instant.now());
        challenge.setExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
        challenge.setFailedAttempts(0);
        when(challengeRepo.findTopByEmailAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                "person@example.com", "REGISTRATION")).thenReturn(Optional.of(challenge));
    }

    @Test
    void accountCannotBeCreatedWithoutVerifiedOtpAndRegistrationToken() {
        CompleteRegistrationRequest request = request("unverified-token");

        assertThrows(ApiException.class, () -> registrationService.completeRegistration(request));
        verify(userRepo, never()).save(any(User.class));
    }

    @Test
    void accountRequiresTheOneTimeTokenReturnedAfterOtpVerification() {
        when(passwordEncoder.matches("123456", "otp-hash")).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenReturn("token-hash");
        when(passwordEncoder.matches(anyString(), eq("token-hash"))).thenAnswer(invocation ->
                invocation.<String>getArgument(0).equals(challengeToken.get()));
        when(passwordEncoder.encode("password123")).thenReturn("password-hash");
        when(userRepo.existsByEmailIgnoreCase("person@example.com")).thenReturn(false);
        when(userRepo.existsByUsernameIgnoreCase("person_1")).thenReturn(false);
        when(userRepo.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(challengeRepo.save(any(OtpChallenge.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String registrationToken = registrationService.verifyRegistrationOtp(
                new VerifyOtpRequest("person@example.com", "123456"));
        challengeToken.set(registrationToken);

        assertThrows(ApiException.class, () -> registrationService.completeRegistration(
                request("wrong-token")));
        verify(userRepo, never()).save(any(User.class));

        User created = registrationService.completeRegistration(
                new CompleteRegistrationRequest("person@example.com", "person_1", "password123", registrationToken));

        assertEquals("person@example.com", created.getEmail());
        assertEquals("password-hash", created.getPassword());
        assertNotNull(challenge.getConsumedAt());
        verify(userRepo).save(created);

        assertThrows(ApiException.class, () -> registrationService.completeRegistration(
                new CompleteRegistrationRequest("person@example.com", "person_1", "password123", registrationToken)));
    }

    private CompleteRegistrationRequest request(String token) {
        return new CompleteRegistrationRequest("person@example.com", "person_1", "password123", token);
    }
}
