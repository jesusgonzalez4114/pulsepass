package com.pulsepass.platform.dto.response;

public record UserResponse(
        Long id,
        String username,
        String email,
        boolean active,
        String firstName,
        String lastName,
        String city
) {
}
