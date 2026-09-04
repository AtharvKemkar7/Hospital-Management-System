package com.healthcare.auth.util;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Generates high-entropy opaque tokens using {@link SecureRandom}.
 *
 * <p>Output is 32 random bytes (256 bits) encoded as URL-safe Base64
 * without padding, which is safe to put in URLs, headers, and JSON.
 */
public final class SecureTokenGenerator {

    private static final SecureRandom RNG = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    private SecureTokenGenerator() {}

    /** Returns a new 256-bit opaque token, URL-safe Base64 encoded. */
    public static String generate() {
        byte[] bytes = new byte[32];
        RNG.nextBytes(bytes);
        return ENCODER.encodeToString(bytes);
    }
}
