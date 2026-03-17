package org.example.bankcardmanagement.transfer.repository;

import org.example.bankcardmanagement.transfer.domain.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TransferRepository extends JpaRepository<Transfer, UUID> {
}
