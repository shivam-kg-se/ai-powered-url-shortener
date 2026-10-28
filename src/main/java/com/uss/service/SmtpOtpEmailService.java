package com.uss.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class SmtpOtpEmailService implements OtpEmailService {

    private static final Logger log =
            LoggerFactory.getLogger(SmtpOtpEmailService.class);

    private final JavaMailSender mailSender;
    private final String from;

    public SmtpOtpEmailService(JavaMailSender mailSender,
                               @Value("${app.otp.mail-from:no-reply@localhost}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void send(String email, String otp, String purpose) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("Your URL Shortener verification code");
        message.setText(
                "Your verification code for " + purpose + " is " + otp
                        + ". It expires in 10 minutes."
        );

        try {
            mailSender.send(message);
            log.info("Verification email sent successfully to {}", email);
        } catch (MailException exception) {
            log.error("Failed to send verification email. Cause: {}",
                    exception.getMessage(), exception);

            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Verification email could not be sent"
            );
        }
    }
}
