package com.velzox.apimonitor.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Encryption Service - Handles encryption/decryption of sensitive credentials
 * 
 * SECURITY DESIGN:
 * - AES-256-GCM encryption (authenticated encryption)
 * - Random IV for each encryption (prevents pattern analysis)
 * - IV stored with ciphertext for decryption
 * - Constant-time operations where possible
 * 
 * USAGE:
 * - Encrypt API keys, tokens, passwords before storing
 * - Decrypt only in memory when making HTTP requests
 * - Never log or expose decrypted values
 */
@Service
@Slf4j
public class EncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;     // 96 bits for GCM
    private static final int GCM_TAG_LENGTH = 128;   // 128-bit authentication tag

    @Value("${app.encryption.secret}")
    private String encryptionSecret;

    private SecretKeySpec secretKey;
    private SecureRandom secureRandom;

    /**
     * Initialize encryption key from configuration
     */
    @PostConstruct
    public void init() {
        // Ensure key is exactly 32 bytes (256 bits) for AES-256
        byte[] keyBytes = new byte[32];
        byte[] secretBytes = encryptionSecret.getBytes();
        
        // Copy secret bytes, padding with zeros if needed
        System.arraycopy(secretBytes, 0, keyBytes, 0, 
                        Math.min(secretBytes.length, keyBytes.length));
        
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
        this.secureRandom = new SecureRandom();
        
        log.info("Encryption service initialized with AES-256-GCM");
    }

    /**
     * Encrypt a plaintext string
     * 
     * @param plaintext The value to encrypt
     * @return Base64-encoded ciphertext (includes IV)
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return null;
        }

        try {
            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            // Initialize cipher for encryption
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

            // Encrypt the plaintext
            byte[] plaintextBytes = plaintext.getBytes();
            byte[] ciphertext = cipher.doFinal(plaintextBytes);

            // Combine IV + ciphertext for storage
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);

            // Return as Base64 string for database storage
            return Base64.getEncoder().encodeToString(buffer.array());

        } catch (Exception e) {
            log.error("Encryption failed", e);
            throw new RuntimeException("Failed to encrypt data", e);
        }
    }

    /**
     * Decrypt a ciphertext string
     * 
     * @param ciphertext Base64-encoded ciphertext (includes IV)
     * @return Decrypted plaintext
     */
    public String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isEmpty()) {
            return null;
        }

        try {
            // Decode from Base64
            byte[] decoded = Base64.getDecoder().decode(ciphertext);

            // Extract IV and ciphertext
            ByteBuffer buffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);
            byte[] encryptedBytes = new byte[buffer.remaining()];
            buffer.get(encryptedBytes);

            // Initialize cipher for decryption
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

            // Decrypt and return
            byte[] plaintextBytes = cipher.doFinal(encryptedBytes);
            return new String(plaintextBytes);

        } catch (Exception e) {
            log.error("Decryption failed", e);
            throw new RuntimeException("Failed to decrypt data", e);
        }
    }

    /**
     * Mask a credential value for display
     * Shows only last 4 characters: "****abcd"
     * 
     * @param value The value to mask
     * @return Masked string
     */
    public String mask(String value) {
        if (value == null || value.length() <= 4) {
            return "****";
        }
        return "****" + value.substring(value.length() - 4);
    }

    /**
     * Securely compare two strings in constant time
     * Prevents timing attacks
     * 
     * @param a First string
     * @param b Second string
     * @return true if strings are equal
     */
    public boolean secureCompare(String a, String b) {
        if (a == null || b == null) {
            return a == b;
        }
        
        byte[] aBytes = a.getBytes();
        byte[] bBytes = b.getBytes();
        
        if (aBytes.length != bBytes.length) {
            return false;
        }
        
        int result = 0;
        for (int i = 0; i < aBytes.length; i++) {
            result |= aBytes[i] ^ bBytes[i];
        }
        
        return result == 0;
    }
}
