package org.example.bankcardmanagement.security.api.dto;

public record AuthResponse(
        String accessToken,
        String tokenType
) {
}
