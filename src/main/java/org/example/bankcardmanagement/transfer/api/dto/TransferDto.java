package org.example.bankcardmanagement.transfer.api.dto;

import org.example.bankcardmanagement.transfer.domain.TransferStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferDto(
        UUID id,
        UUID fromCardId,
        UUID toCardId,
        BigDecimal amount,
        Instant createdAt,
        String createdBy,
        TransferStatus status
) {
}
