package org.example.bankcardmanagement.admin.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.bankcardmanagement.admin.api.dto.AdminUserDto;
import org.example.bankcardmanagement.admin.api.dto.UserEnabledUpdateRequest;
import org.example.bankcardmanagement.admin.api.dto.UserRolesUpdateRequest;
import org.example.bankcardmanagement.admin.service.AdminUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public Page<AdminUserDto> list(@PageableDefault(size = 20) Pageable pageable) {
        log.info("HTTP GET /api/admin/users page={} size={}", pageable.getPageNumber(), pageable.getPageSize());
        return adminUserService.listUsers(pageable);
    }

    @PatchMapping("/{id}/enabled")
    public AdminUserDto setEnabled(@PathVariable UUID id, @Valid @RequestBody UserEnabledUpdateRequest request) {
        log.info("HTTP PATCH /api/admin/users/{}/enabled enabled={}", id, request.enabled());
        return adminUserService.setEnabled(id, request.enabled());
    }

    @PatchMapping("/{id}/roles")
    public AdminUserDto setRoles(@PathVariable UUID id, @Valid @RequestBody UserRolesUpdateRequest request) {
        log.info("HTTP PATCH /api/admin/users/{}/roles roles={}", id, request.roles());
        return adminUserService.setRoles(id, request.roles());
    }
}
