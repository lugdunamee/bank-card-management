package org.example.bankcardmanagement.card.api.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import org.example.bankcardmanagement.card.domain.CardStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CardRequestDto(
        @NotBlank
        @Pattern(regexp = "^[0-9 ]{13,19}$")
        String cardNumber,

        @NotBlank
        String owner,

        @NotNull
        @Future
        LocalDate expiryDate,

        @NotNull
        CardStatus status,

        @NotNull
        @PositiveOrZero
        BigDecimal balance
) {
}
