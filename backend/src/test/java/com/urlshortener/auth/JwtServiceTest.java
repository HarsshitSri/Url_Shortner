package com.urlshortener.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JwtServiceTest {

    @Test
    void generateAndParseToken() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("unit-test-secret-key-at-least-32-bytes-long");
        properties.setExpirationMs(60_000L);
        JwtService jwtService = new JwtService(properties);

        User user = new User(java.util.UUID.randomUUID(), "jwt@example.com", "hash");
        String token = jwtService.generateToken(user);

        assertThat(jwtService.isValid(token)).isTrue();
        assertThat(jwtService.extractUserId(token)).isEqualTo(user.getId());
        assertThat(jwtService.extractEmail(token)).isEqualTo("jwt@example.com");
    }
}
