package org.example.bankcardmanagement.card.service;

import org.example.bankcardmanagement.card.api.dto.CardDto;
import org.example.bankcardmanagement.card.api.dto.CardRequestDto;
import org.example.bankcardmanagement.card.domain.Card;
import org.example.bankcardmanagement.card.domain.CardStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Card management operations.
 */
public interface CardService {

    CardDto create(CardRequestDto dto);

    Page<CardDto> searchByOwner(String ownerQuery, Pageable pageable);

    Page<CardDto> searchMyCards(String owner, Pageable pageable);

    Card getById(UUID id);

    void delete(UUID id);

    CardDto updateStatus(UUID id, CardStatus status);

    CardDto requestBlock(UUID id);
}
