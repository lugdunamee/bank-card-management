package org.example.bankcardmanagement.card.api.dto;

import org.example.bankcardmanagement.card.domain.CardStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CardCreateDto(
        String numberEncrypted,
        String owner,
        LocalDate expiryDate,
        CardStatus status,
        BigDecimal balance
) {
}
