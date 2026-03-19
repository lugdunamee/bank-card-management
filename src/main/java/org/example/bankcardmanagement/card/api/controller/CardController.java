package org.example.bankcardmanagement.card.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.bankcardmanagement.card.api.dto.CardDto;
import org.example.bankcardmanagement.card.api.dto.CardRequestDto;
import org.example.bankcardmanagement.card.api.dto.CardStatusUpdateRequest;
import org.example.bankcardmanagement.card.api.mapper.CardMapper;
import org.example.bankcardmanagement.card.domain.Card;
import org.example.bankcardmanagement.card.service.CardService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Cards API.
 */
@Slf4j
@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;
    private final CardMapper cardMapper;

    /**
     * Creates a new card.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CardDto create(@Valid @RequestBody CardRequestDto request) {
        log.info("HTTP POST /api/cards owner={} expiryDate={} status={}", request.owner(), request.expiryDate(), request.status());
        return cardService.create(request);
    }

    /**
     * Searches cards.
     */
    @GetMapping
    public Page<CardDto> search(
            @RequestParam(value = "owner", required = false) String owner,
            Authentication authentication,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        String username = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN"));

        if (isAdmin) {
            String ownerQuery = (owner == null || owner.isBlank()) ? "" : owner;
            log.info("HTTP GET /api/cards (admin) ownerQuery={} page={} size={}", ownerQuery, pageable.getPageNumber(), pageable.getPageSize());
            return cardService.searchByOwner(ownerQuery, pageable);
        }

        log.info("HTTP GET /api/cards (user) owner={} page={} size={}", username, pageable.getPageNumber(), pageable.getPageSize());
        return cardService.searchMyCards(username, pageable);
    }

    /**
     * Returns a card by id.
     */
    @GetMapping("/{id}")
    public CardDto getById(@PathVariable UUID id, Authentication authentication) {
        Card card = cardService.getById(id);
        enforceAccess(card, authentication);
        return cardMapper.toDto(card);
    }

    /**
     * Deletes a card.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        cardService.delete(id);
    }

    /**
     * Updates card status.
     */
    @PatchMapping("/{id}/status")
    public CardDto updateStatus(@PathVariable UUID id, @Valid @RequestBody CardStatusUpdateRequest request) {
        return cardService.updateStatus(id, request.status());
    }

    /**
     * Requests card blocking.
     */
    @PostMapping("/{id}/block-request")
    public CardDto requestBlock(@PathVariable UUID id, Authentication authentication) {
        Card card = cardService.getById(id);
        enforceAccess(card, authentication);
        return cardService.requestBlock(id);
    }

    private void enforceAccess(Card card, Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN"));
        if (isAdmin) {
            return;
        }

        String username = authentication.getName();
        if (!card.getOwnerUser().getUsername().equalsIgnoreCase(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
    }
}
