package org.example.bankcardmanagement.security.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.bankcardmanagement.security.api.dto.AuthLoginRequest;
import org.example.bankcardmanagement.security.api.dto.AuthRegisterRequest;
import org.example.bankcardmanagement.security.api.dto.AuthResponse;
import org.example.bankcardmanagement.security.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication API.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Registers a new user.
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@Valid @RequestBody AuthRegisterRequest request) {
        log.info("HTTP POST /api/auth/register username={}", request.username());
        authService.registerUser(request.username(), request.password());
    }

    /**
     * Performs login and returns an access token.
     */
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthLoginRequest request) {
        log.info("HTTP POST /api/auth/login username={}", request.username());
        String token = authService.login(request.username(), request.password());
        return new AuthResponse(token, "Bearer");
    }
}
