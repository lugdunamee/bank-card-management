package org.example.bankcardmanagement.transfer.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.bankcardmanagement.transfer.api.dto.TransferDto;
import org.example.bankcardmanagement.transfer.api.dto.TransferRequestDto;
import org.example.bankcardmanagement.transfer.service.TransferService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Transfers API.
 */
@Slf4j
@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    /**
     * Creates a money transfer.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransferDto create(@Valid @RequestBody TransferRequestDto request, Authentication authentication) {
        String username = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN"));

        log.info("HTTP POST /api/transfers fromCardId={} toCardId={} amount={} createdBy={}", request.fromCardId(), request.toCardId(), request.amount(), username);
        return transferService.createTransfer(request, username, isAdmin);
    }
}
