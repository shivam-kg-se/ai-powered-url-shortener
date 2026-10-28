package com.uss.controller;

import com.uss.bindings.LoginRequest;
import com.uss.bindings.ApiMessageResponse;
import com.uss.bindings.CompleteRegistrationRequest;
import com.uss.bindings.SendOtpRequest;
import com.uss.bindings.VerifyOtpRequest;
import com.uss.models.User;
import com.uss.service.JwtAuthenticationResponse;
import com.uss.service.ApiException;
import com.uss.service.RegistrationService;
import com.uss.service.UserService;
import lombok.AllArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
public class AuthController {

    private UserService userService;
    private RegistrationService registrationService;

    @PostMapping("/public/send-otp")
    public ResponseEntity<ApiMessageResponse> sendRegistrationOtp(@Valid @RequestBody SendOtpRequest request) {
        registrationService.sendRegistrationOtp(request.email());
        return ResponseEntity.accepted().body(new ApiMessageResponse("Verification code sent"));
    }

    @PostMapping("/public/verify-otp")
    public ResponseEntity<?> verifyRegistrationOtp(@Valid @RequestBody VerifyOtpRequest request) {
        String registrationToken = registrationService.verifyRegistrationOtp(request);
        if (registrationToken == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Verification code is invalid, expired, or exhausted");
        }
        return ResponseEntity.ok(java.util.Map.of("verified", true, "registrationToken", registrationToken));
    }

    @PostMapping("/public/complete-registration")
    public ResponseEntity<?> completeRegistration(@Valid @RequestBody CompleteRegistrationRequest request) {
        User user = registrationService.completeRegistration(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new com.uss.bindings.UserProfileResponse(user.getId(), user.getEmail(), user.getUsername(), false));
    }

    @PostMapping("/public/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginRequest loginRequest){
        JwtAuthenticationResponse response = userService.loginUser(loginRequest);
        return ResponseEntity.ok(response);
    }
}
