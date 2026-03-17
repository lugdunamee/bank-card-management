package org.example.bankcardmanagement.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.bankcardmanagement.security.domain.AppUser;
import org.example.bankcardmanagement.security.domain.Role;
import org.example.bankcardmanagement.security.repository.AppUserRepository;
import org.example.bankcardmanagement.security.repository.RoleRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final PasswordService passwordService;

    @Override
    public void run(ApplicationArguments args) {
        String username = System.getenv("APP_ADMIN_USERNAME");
        String password = System.getenv("APP_ADMIN_PASSWORD");

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return;
        }

        if (appUserRepository.existsByUsernameIgnoreCase(username)) {
            return;
        }

        Role adminRole = roleRepository.findByName(RoleInitializer.ROLE_ADMIN)
                .orElseThrow(() -> new IllegalStateException("ROLE_ADMIN is not initialized"));

        AppUser admin = new AppUser();
        admin.setUsername(username);
        admin.setPasswordHash(passwordService.hash(password));
        admin.setEnabled(true);
        admin.setCreatedAt(Instant.now());
        admin.setRoles(Set.of(adminRole));

        appUserRepository.save(admin);
        log.info("Bootstrap admin created username={}", username);
    }
}
