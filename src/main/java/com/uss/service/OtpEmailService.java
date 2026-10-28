package com.uss.service;

public interface OtpEmailService {
    void send(String email, String otp, String purpose);
}
