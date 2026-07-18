package io.chicaodw.platform.common.security;

import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class SecureTokenGeneratorTest {

    @Test
    void generate_producesAUrlSafeToken_withNoPaddingCharacters() {
        String token = SecureTokenGenerator.generate();
        assertThat(token).doesNotContain("+", "/", "=");
        assertThat(token).matches("[A-Za-z0-9_-]+");
    }

    @Test
    void generate_hasAtLeast128BitsOfEntropy() {
        // 20 random bytes = 160 bits, encoded without padding as Base64url.
        String token = SecureTokenGenerator.generate();
        byte[] decoded = Base64.getUrlDecoder().decode(token);
        assertThat(decoded.length * 8).isGreaterThanOrEqualTo(128);
    }

    @Test
    void generate_isNeverSequentialOrPredictable_acrossManyCalls() {
        Set<String> tokens = new HashSet<>();
        IntStream.range(0, 1000).forEach(i -> tokens.add(SecureTokenGenerator.generate()));
        assertThat(tokens).hasSize(1000);
    }
}
