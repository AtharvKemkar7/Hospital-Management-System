package com.healthcare.auth.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HashUtilTest {

    @Test
    void sha256_isDeterministic() {
        String h1 = HashUtil.sha256("hello");
        String h2 = HashUtil.sha256("hello");
        assertThat(h1).isEqualTo(h2);
    }

    @Test
    void sha256_producesHexString() {
        String h = HashUtil.sha256("hello");
        assertThat(h).hasSize(64).matches("[0-9a-f]+");
    }

    @Test
    void sha256_isCaseSensitive() {
        assertThat(HashUtil.sha256("hello")).isNotEqualTo(HashUtil.sha256("HELLO"));
    }

    @Test
    void sha256_rejectsNull() {
        assertThatThrownBy(() -> HashUtil.sha256(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void knownVector_sha256OfAbc() {
        // NIST test vector
        assertThat(HashUtil.sha256("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }
}
