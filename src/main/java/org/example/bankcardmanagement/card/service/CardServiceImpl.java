package org.example.bankcardmanagement.card.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.bankcardmanagement.card.api.dto.CardDto;
import org.example.bankcardmanagement.card.api.dto.CardRequestDto;
import org.example.bankcardmanagement.card.api.mapper.CardMapper;
import org.example.bankcardmanagement.card.domain.Card;
import org.example.bankcardmanagement.card.domain.CardStatus;
import org.example.bankcardmanagement.card.repository.CardRepository;
import org.example.bankcardmanagement.common.crypto.CardCryptoService;
import org.example.bankcardmanagement.security.domain.AppUser;
import org.example.bankcardmanagement.security.repository.AppUserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Default implementation of {@link CardService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CardServiceImpl implements CardService {

    private final CardRepository cardRepository;
    private final CardMapper cardMapper;
    private final CardCryptoService cardCryptoService;
    private final AppUserRepository appUserRepository;

    /**
     * Creates a new card.
     */
    @Override
    @Transactional
    public CardDto create(CardRequestDto dto) {
        log.info("Creating card for owner={} expiryDate={} status={}", dto.owner(), dto.expiryDate(), dto.status());

        Card card = cardMapper.toEntity(dto);

        AppUser owner = appUserRepository.findByUsernameIgnoreCase(dto.owner())
                .orElseThrow(() -> new IllegalArgumentException("Owner user not found"));
        card.setOwnerUser(owner);

        String digits = dto.cardNumber().replaceAll("\\s+", "");
        String last4 = digits.length() <= 4 ? digits : digits.substring(digits.length() - 4);
        String encrypted = cardCryptoService.encrypt(digits);

        card.setNumberLast4(last4);
        card.setNumberEncrypted(encrypted);

        Card saved = cardRepository.save(card);

        return cardMapper.toDto(saved);
    }

    /**
     * Searches cards by owner username (admin operation).
     */
    @Override
    @Transactional(readOnly = true)
    public Page<CardDto> searchByOwner(String ownerQuery, Pageable pageable) {
        log.info("Searching cards by ownerQuery={} page={} size={}", ownerQuery, pageable.getPageNumber(), pageable.getPageSize());

        return cardRepository.findByOwnerUserUsernameContainingIgnoreCase(ownerQuery, pageable)
                .map(cardMapper::toDto);
    }

    /**
     * Searches cards for the specified owner.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<CardDto> searchMyCards(String owner, Pageable pageable) {
        log.info("Searching my cards owner={} page={} size={}", owner, pageable.getPageNumber(), pageable.getPageSize());

        return cardRepository.findByOwnerUserUsernameIgnoreCase(owner, pageable)
                .map(cardMapper::toDto);
    }

    /**
     * Returns card entity by id.
     */
    @Override
    @Transactional(readOnly = true)
    public Card getById(UUID id) {
        return cardRepository.findWithOwnerUserById(id)
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));
    }

    /**
     * Deletes a card.
     */
    @Override
    @Transactional
    public void delete(UUID id) {
        Card card = getById(id);
        cardRepository.delete(card);
    }

    /**
     * Updates card status.
     */
    @Override
    @Transactional
    public CardDto updateStatus(UUID id, CardStatus status) {
        Card card = getById(id);
        card.setStatus(status);
        return cardMapper.toDto(cardRepository.save(card));
    }

    /**
     * Requests to block the card.
     */
    @Override
    @Transactional
    public CardDto requestBlock(UUID id) {
        Card card = getById(id);
        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new IllegalArgumentException("Card is not active");
        }
        card.setStatus(CardStatus.BLOCK_REQUESTED);
        return cardMapper.toDto(cardRepository.save(card));
    }
}
