package org.example.bankcardmanagement.card.api.dto;

import org.example.bankcardmanagement.card.domain.CardStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CardDto(
        UUID id,
        String maskedNumber,
        String owner,
        LocalDate expiryDate,
        CardStatus status,
        BigDecimal balance
) {
}
