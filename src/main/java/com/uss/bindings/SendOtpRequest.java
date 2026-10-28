package com.uss.bindings;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SendOtpRequest(@NotBlank @Email String email) {
}
