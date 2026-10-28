package com.uss.bindings;

public record UserProfileResponse(Long id, String email, String username, boolean hasProfilePicture) {
}
