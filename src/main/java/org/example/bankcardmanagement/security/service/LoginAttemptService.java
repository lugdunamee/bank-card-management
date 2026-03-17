package org.example.bankcardmanagement.security.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(10);
    private static final Duration BLOCK_DURATION = Duration.ofMinutes(15);

    private final ConcurrentHashMap<String, Attempts> attemptsByUsername = new ConcurrentHashMap<>();

    public void onSuccess(String username) {
        if (username == null) {
            return;
        }
        attemptsByUsername.remove(normalize(username));
    }

    public void onFailure(String username) {
        if (username == null) {
            return;
        }

        String key = normalize(username);
        attemptsByUsername.compute(key, (k, current) -> {
            Instant now = Instant.now();
            Attempts a = (current == null) ? new Attempts(now, 0, null) : current;

            if (a.firstAttemptAt != null && now.isAfter(a.firstAttemptAt.plus(WINDOW))) {
                a = new Attempts(now, 0, null);
            }

            int newCount = a.count + 1;
            Instant blockedUntil = a.blockedUntil;
            if (newCount >= MAX_ATTEMPTS) {
                blockedUntil = now.plus(BLOCK_DURATION);
            }

            return new Attempts(a.firstAttemptAt == null ? now : a.firstAttemptAt, newCount, blockedUntil);
        });
    }

    public void checkNotBlocked(String username) {
        if (username == null) {
            return;
        }

        Attempts a = attemptsByUsername.get(normalize(username));
        if (a == null || a.blockedUntil == null) {
            return;
        }

        if (Instant.now().isBefore(a.blockedUntil)) {
            throw new IllegalStateException("Too many login attempts. Try later.");
        }
    }

    private String normalize(String username) {
        return username.trim().toLowerCase();
    }

    private record Attempts(Instant firstAttemptAt, int count, Instant blockedUntil) {
    }
}
