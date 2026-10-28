package com.uss.controller;

import com.uss.bindings.*;
import com.uss.models.User;
import com.uss.service.ProfilePictureStorage;
import com.uss.service.ProfileService;
import com.uss.service.RegistrationService;
import com.uss.service.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/users/me")
public class UserController {
    private final ProfileService profileService;
    private final RegistrationService registrationService;

    public UserController(ProfileService profileService, RegistrationService registrationService) {
        this.profileService = profileService;
        this.registrationService = registrationService;
    }

    @GetMapping
    public UserProfileResponse getProfile(@AuthenticationPrincipal UserDetailsImpl principal) {
        return profileService.getProfile(authenticatedUser(principal));
    }

    @PatchMapping
    public UserProfileResponse updateProfile(@AuthenticationPrincipal UserDetailsImpl principal,
                                             @Valid @RequestBody UpdateProfileRequest request) {
        return profileService.updateProfile(authenticatedUser(principal), request);
    }

    @PostMapping(value = "/profile-picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiMessageResponse> uploadProfilePicture(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @RequestPart("file") MultipartFile file) throws IOException {
        profileService.uploadProfilePicture(authenticatedUser(principal), file);
        return ResponseEntity.ok(new ApiMessageResponse("Profile picture uploaded"));
    }

    @GetMapping("/profile-picture")
    public ResponseEntity<byte[]> getProfilePicture(@AuthenticationPrincipal UserDetailsImpl principal)
            throws IOException {
        ProfilePictureStorage.StoredPicture picture =
                profileService.getProfilePicture(authenticatedUser(principal));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(picture.contentType()))
                .cacheControl(CacheControl.noStore())
                .body(picture.content());
    }

    @DeleteMapping("/profile-picture")
    public ResponseEntity<Void> deleteProfilePicture(@AuthenticationPrincipal UserDetailsImpl principal)
            throws IOException {
        profileService.deleteProfilePicture(authenticatedUser(principal));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/password")
    public ResponseEntity<ApiMessageResponse> changePassword(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        profileService.changePassword(authenticatedUser(principal), request);
        return ResponseEntity.ok(new ApiMessageResponse("Password changed successfully"));
    }

    @PostMapping("/email-change/send-otp")
    public ResponseEntity<ApiMessageResponse> sendEmailChangeOtp(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @Valid @RequestBody SendOtpRequest request) {
        registrationService.sendEmailChangeOtp(authenticatedUser(principal), request.email());
        return ResponseEntity.accepted().body(new ApiMessageResponse("Verification code sent"));
    }

    @PostMapping("/email-change/verify-otp")
    public ResponseEntity<?> verifyEmailChangeOtp(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @Valid @RequestBody VerifyOtpRequest request) {
        User user = registrationService.verifyEmailChangeOtp(authenticatedUser(principal), request);
        if (user == null) {
            throw new com.uss.service.ApiException(HttpStatus.BAD_REQUEST,
                    "Verification code is invalid, expired, or exhausted");
        }
        return ResponseEntity.ok(Map.of("email", user.getEmail(), "verified", true));
    }

    private User authenticatedUser(UserDetailsImpl principal) {
        return profileService.findAuthenticatedUser(principal.getId());
    }
}
