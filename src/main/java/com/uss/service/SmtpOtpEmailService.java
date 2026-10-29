
package com.uss.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class SmtpOtpEmailService implements OtpEmailService {

    private static final Logger log =
            LoggerFactory.getLogger(SmtpOtpEmailService.class);

    private final Resend resend;
    private final String from;

    public SmtpOtpEmailService(
            @Value("${resend.api-key}") String apiKey,
            @Value("${resend.from-email:onboarding@resend.dev}") String from) {

        this.resend = new Resend(apiKey);
        this.from = from;
    }

    @Override
    public void send(String email, String otp, String purpose) {

        String message =
                "Hello from Linkora-AI!\n\n"
                        + "Your verification code for " + purpose + " is: "
                        + otp + ".\n\n"
                        + "This code is valid for 10 minutes only. "
                        + "Please do not share it with anyone.\n\n"
                        + "If you did not request this code, please ignore "
                        + "this email.\n\n"
                        + "Best regards,\n"
                        + "Shivam Kumar Gupta\n"
                        + "Founder, Linkora-AI";

        try {
            CreateEmailOptions options =
                    CreateEmailOptions.builder()
                            .from(from)
                            .to(email)
                            .subject("Your Linkora-AI verification code")
                            .text(message)
                            .build();

            resend.emails().send(options);

            log.info("Verification email sent successfully");

        } catch (ResendException | RuntimeException exception) {
            // Do not log the OTP, recipient or API key.
            log.error("Failed to send verification email: {}",
                    exception.getMessage());

            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Verification email could not be sent"
            );
        }
    }
}
