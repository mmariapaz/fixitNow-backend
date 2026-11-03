package com.fixitnow.backend.auth;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}
