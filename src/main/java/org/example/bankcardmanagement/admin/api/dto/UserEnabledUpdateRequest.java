package org.example.bankcardmanagement.admin.api.dto;

import jakarta.validation.constraints.NotNull;

public record UserEnabledUpdateRequest(
        @NotNull
        Boolean enabled
) {
}
