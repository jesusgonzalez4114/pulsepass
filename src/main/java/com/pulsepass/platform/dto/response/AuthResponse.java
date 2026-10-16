package com.pulsepass.platform.dto.response;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {

    /** El token es una credencial: no debe terminar en un log. */
    @Override
    public String toString() {
        return "AuthResponse[accessToken=****, tokenType=" + tokenType + ", expiresIn=" + expiresIn + "]";
    }
}
