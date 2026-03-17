package org.example.bankcardmanagement.card.api.dto;

import jakarta.validation.constraints.NotNull;
import org.example.bankcardmanagement.card.domain.CardStatus;

public record CardStatusUpdateRequest(
        @NotNull
        CardStatus status
) {
}
