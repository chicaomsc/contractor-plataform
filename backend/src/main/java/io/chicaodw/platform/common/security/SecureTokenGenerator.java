package io.chicaodw.platform.common.security;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Generates unpredictable, URL-safe opaque tokens (share links, etc.) — never sequential,
 * never derived from a UUID (which can be enumerable/predictable depending on the generator).
 * 20 random bytes = 160 bits of entropy, comfortably above the 128-bit minimum.
 */
public final class SecureTokenGenerator {

    private static final int TOKEN_BYTES = 20;
    private static final SecureRandom RANDOM = new SecureRandom();

    private SecureTokenGenerator() {
    }

    public static String generate() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
