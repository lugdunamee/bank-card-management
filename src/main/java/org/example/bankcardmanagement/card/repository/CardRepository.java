package org.example.bankcardmanagement.card.repository;

import org.example.bankcardmanagement.card.domain.Card;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

public interface CardRepository extends JpaRepository<Card, UUID> {

    @EntityGraph(attributePaths = "ownerUser")
    Page<Card> findByOwnerUserUsernameContainingIgnoreCase(String owner, Pageable pageable);

    @EntityGraph(attributePaths = "ownerUser")
    Page<Card> findByOwnerUserUsernameIgnoreCase(String owner, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "ownerUser")
    Optional<Card> findWithLockById(UUID id);

    @EntityGraph(attributePaths = "ownerUser")
    Optional<Card> findWithOwnerUserById(UUID id);
}
