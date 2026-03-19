package org.example.bankcardmanagement.security.service;

import org.springframework.security.core.AuthenticationException;
/**
 * Authentication and registration operations.
 */
public interface AuthService {

    void registerUser(String username, String rawPassword);

    String login(String username, String password) throws AuthenticationException;
}
