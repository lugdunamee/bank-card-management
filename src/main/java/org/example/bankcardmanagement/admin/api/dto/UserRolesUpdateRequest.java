package org.example.bankcardmanagement.admin.api.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record UserRolesUpdateRequest(
        @NotEmpty
        Set<String> roles
) {
}
