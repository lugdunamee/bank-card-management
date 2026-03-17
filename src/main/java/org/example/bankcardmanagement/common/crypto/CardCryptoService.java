package org.example.bankcardmanagement.common.crypto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardCryptoService {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final CardCryptoProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }

        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey(), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + cipherText.length);
            buffer.put(iv);
            buffer.put(cipherText);

            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (GeneralSecurityException e) {
            log.error("Encryption failed", e);
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    public String decrypt(String encryptedBase64) {
        if (encryptedBase64 == null) {
            return null;
        }

        try {
            byte[] allBytes = Base64.getDecoder().decode(encryptedBase64);
            if (allBytes.length <= IV_LENGTH_BYTES) {
                throw new IllegalArgumentException("Encrypted value is too short");
            }

            byte[] iv = new byte[IV_LENGTH_BYTES];
            byte[] cipherText = new byte[allBytes.length - IV_LENGTH_BYTES];
            System.arraycopy(allBytes, 0, iv, 0, IV_LENGTH_BYTES);
            System.arraycopy(allBytes, IV_LENGTH_BYTES, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] plainBytes = cipher.doFinal(cipherText);

            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            log.error("Decryption failed", e);
            throw new IllegalStateException("Decryption failed", e);
        }
    }

    private SecretKey secretKey() {
        String secretBase64 = properties.getSecret();
        if (secretBase64 == null || secretBase64.isBlank()) {
            throw new IllegalStateException("app.crypto.card.secret is not configured");
        }

        byte[] keyBytes = Base64.getDecoder().decode(secretBase64);
        if (keyBytes.length != 32) {
            throw new IllegalStateException("app.crypto.card.secret must be a Base64-encoded 256-bit (32 bytes) key");
        }

        return new SecretKeySpec(keyBytes, "AES");
    }
}
