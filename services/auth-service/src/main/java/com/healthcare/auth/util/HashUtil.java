package com.healthcare.auth.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Hashing helpers. Currently exposes a SHA-256 of arbitrary UTF-8 input.
 *
 * <p>SHA-256 is the correct choice for storing <em>opaque</em> tokens whose
 * entropy already comes from a CSPRNG. It is NOT a password-hashing
 * algorithm: passwords are hashed with BCrypt, never SHA-256.
 */
public final class HashUtil {

    private HashUtil() {}

    public static String sha256(String input) {
        if (input == null) {
            throw new IllegalArgumentException("input must not be null");
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return toHex(digest);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is mandated by the JRE; this branch is unreachable.
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}
