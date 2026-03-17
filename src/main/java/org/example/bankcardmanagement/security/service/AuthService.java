package org.example.bankcardmanagement.security.service;

import lombok.RequiredArgsConstructor;
import org.example.bankcardmanagement.security.domain.AppUser;
import org.example.bankcardmanagement.security.domain.Role;
import org.example.bankcardmanagement.security.repository.AppUserRepository;
import org.example.bankcardmanagement.security.repository.RoleRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final PasswordService passwordService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;
    private final RegistrationAttemptService registrationAttemptService;

    @Transactional
    public void registerUser(String username, String rawPassword) {
        registrationAttemptService.checkNotBlocked(username);

        if (appUserRepository.existsByUsernameIgnoreCase(username)) {
            registrationAttemptService.onFailure(username);
            return;
        }

        Role userRole = roleRepository.findByName(RoleInitializer.ROLE_USER)
                .orElseThrow(() -> new IllegalStateException("ROLE_USER is not initialized"));

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(passwordService.hash(rawPassword));
        user.setEnabled(true);
        user.setCreatedAt(Instant.now());
        user.setRoles(Set.of(userRole));

        appUserRepository.save(user);
        registrationAttemptService.onSuccess(username);
    }

    public String login(String username, String password) throws AuthenticationException {
        loginAttemptService.checkNotBlocked(username);
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );

            loginAttemptService.onSuccess(username);
            return jwtService.generateAccessToken(auth.getName());
        } catch (AuthenticationException ex) {
            loginAttemptService.onFailure(username);
            throw ex;
        }
    }
}
