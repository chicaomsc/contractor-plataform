package io.chicaodw.platform.common.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenHasherTest {

    @Test
    void sha256Hex_sameInput_producesSameHash() {
        String token = "xh83JSkLm82AexampleToken";
        assertThat(TokenHasher.sha256Hex(token)).isEqualTo(TokenHasher.sha256Hex(token));
    }

    @Test
    void sha256Hex_differentInputs_produceDifferentHashes() {
        assertThat(TokenHasher.sha256Hex("token-a")).isNotEqualTo(TokenHasher.sha256Hex("token-b"));
    }

    @Test
    void sha256Hex_producesA64CharacterLowercaseHexString() {
        String hash = TokenHasher.sha256Hex("any-token-value");
        assertThat(hash).hasSize(64).matches("[0-9a-f]{64}");
    }

    @Test
    void sha256Hex_doesNotReturnTheRawInput() {
        String token = "raw-secret-token";
        assertThat(TokenHasher.sha256Hex(token)).isNotEqualTo(token).doesNotContain(token);
    }
}
