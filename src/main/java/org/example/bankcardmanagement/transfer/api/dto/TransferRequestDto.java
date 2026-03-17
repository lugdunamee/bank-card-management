package org.example.bankcardmanagement.transfer.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequestDto(
        @NotNull
        UUID fromCardId,

        @NotNull
        UUID toCardId,

        @NotNull
        @Positive
        BigDecimal amount
) {
}
