package org.example.bankcardmanagement.admin.api.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record AdminUserDto(
        UUID id,
        String username,
        boolean enabled,
        Instant createdAt,
        Set<String> roles
) {
}
