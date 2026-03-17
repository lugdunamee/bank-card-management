package org.example.bankcardmanagement.transfer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.bankcardmanagement.card.domain.Card;
import org.example.bankcardmanagement.card.domain.CardStatus;
import org.example.bankcardmanagement.card.repository.CardRepository;
import org.example.bankcardmanagement.transfer.api.dto.TransferDto;
import org.example.bankcardmanagement.transfer.api.dto.TransferRequestDto;
import org.example.bankcardmanagement.transfer.domain.Transfer;
import org.example.bankcardmanagement.transfer.domain.TransferStatus;
import org.example.bankcardmanagement.transfer.repository.TransferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final CardRepository cardRepository;
    private final TransferRepository transferRepository;

    @Transactional
    public TransferDto createTransfer(TransferRequestDto dto, String principal, boolean isAdmin) {
        if (dto.fromCardId().equals(dto.toCardId())) {
            throw new IllegalArgumentException("fromCardId and toCardId must be different");
        }

        BigDecimal amount = dto.amount();
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }

        UUID first = dto.fromCardId().compareTo(dto.toCardId()) <= 0 ? dto.fromCardId() : dto.toCardId();
        UUID second = first.equals(dto.fromCardId()) ? dto.toCardId() : dto.fromCardId();

        Card card1 = cardRepository.findWithLockById(first)
                .orElseThrow(() -> new IllegalArgumentException("Card not found: " + first));
        Card card2 = cardRepository.findWithLockById(second)
                .orElseThrow(() -> new IllegalArgumentException("Card not found: " + second));

        Card from = dto.fromCardId().equals(card1.getId()) ? card1 : card2;
        Card to = dto.toCardId().equals(card1.getId()) ? card1 : card2;

        if (!from.getOwnerUser().getId().equals(to.getOwnerUser().getId())) {
            throw new IllegalArgumentException("Transfers are allowed only between cards of the same owner");
        }

        if (!isAdmin) {
            String ownerUsername = from.getOwnerUser().getUsername();
            if (!ownerUsername.equalsIgnoreCase(principal)) {
                throw new IllegalArgumentException("Transfers are allowed only between your own cards");
            }
        }

        validateCardEligible(from);
        validateCardEligible(to);

        if (from.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds");
        }

        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));

        cardRepository.save(from);
        cardRepository.save(to);

        Transfer transfer = new Transfer();
        transfer.setFromCardId(from.getId());
        transfer.setToCardId(to.getId());
        transfer.setAmount(amount);
        transfer.setCreatedAt(Instant.now());
        transfer.setCreatedBy(principal);
        transfer.setStatus(TransferStatus.SUCCESS);

        Transfer saved = transferRepository.save(transfer);

        log.info("Transfer created id={} fromCardId={} toCardId={} amount={} createdBy={}", saved.getId(), from.getId(), to.getId(), amount, principal);

        return new TransferDto(
                saved.getId(),
                saved.getFromCardId(),
                saved.getToCardId(),
                saved.getAmount(),
                saved.getCreatedAt(),
                saved.getCreatedBy(),
                saved.getStatus()
        );
    }

    private void validateCardEligible(Card card) {
        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new IllegalArgumentException("Card is not active: " + card.getId());
        }

        LocalDate expiry = card.getExpiryDate();
        if (expiry != null && expiry.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Card is expired: " + card.getId());
        }
    }
}
