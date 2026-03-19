package org.example.bankcardmanagement.transfer.service;

import org.example.bankcardmanagement.transfer.api.dto.TransferDto;
import org.example.bankcardmanagement.transfer.api.dto.TransferRequestDto;
/**
 * Money transfer operations between cards.
 */
public interface TransferService {

    TransferDto createTransfer(TransferRequestDto dto, String principal, boolean isAdmin);
}
