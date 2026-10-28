package com.uss.bindings;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CompleteRegistrationRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "^[a-zA-Z0-9._-]+$") String username,
        @NotBlank @Size(min = 8, max = 128) String password,
        @NotBlank String registrationToken) {
}
