package org.example.bankcardmanagement.admin.service;

import lombok.RequiredArgsConstructor;
import org.example.bankcardmanagement.admin.api.dto.AdminUserDto;
import org.example.bankcardmanagement.security.domain.AppUser;
import org.example.bankcardmanagement.security.domain.Role;
import org.example.bankcardmanagement.security.repository.AppUserRepository;
import org.example.bankcardmanagement.security.repository.RoleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;

    @Transactional(readOnly = true)
    public Page<AdminUserDto> listUsers(Pageable pageable) {
        return appUserRepository.findAll(pageable)
                .map(this::toDto);
    }

    @Transactional
    public AdminUserDto setEnabled(UUID userId, boolean enabled) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setEnabled(enabled);
        return toDto(appUserRepository.save(user));
    }

    @Transactional
    public AdminUserDto setRoles(UUID userId, Set<String> roleNames) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Set<Role> roles = roleNames.stream()
                .map(name -> roleRepository.findByName(name)
                        .orElseThrow(() -> new IllegalArgumentException("Role not found: " + name)))
                .collect(java.util.stream.Collectors.toSet());

        user.setRoles(roles);
        return toDto(appUserRepository.save(user));
    }

    private AdminUserDto toDto(AppUser user) {
        Set<String> roles = user.getRoles().stream().map(Role::getName).collect(java.util.stream.Collectors.toSet());
        return new AdminUserDto(user.getId(), user.getUsername(), user.isEnabled(), user.getCreatedAt(), roles);
    }
}
