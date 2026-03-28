package com.smartlift.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    // 256-bit secret for HMAC-SHA
    private static final String SECRET = "my-super-secret-key-for-jwt-testing-256-bits!!";
    private static final long EXPIRATION = 3600000L; // 1 hour

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(SECRET, EXPIRATION);
    }

    @Test
    void generateToken_returnsNonEmptyToken() {
        Authentication auth = createAuthentication("testuser");

        String token = jwtTokenProvider.generateToken(auth);

        assertThat(token).isNotNull().isNotEmpty();
    }

    @Test
    void getUsernameFromToken_returnsCorrectUsername() {
        Authentication auth = createAuthentication("admin");
        String token = jwtTokenProvider.generateToken(auth);

        String username = jwtTokenProvider.getUsernameFromToken(token);

        assertThat(username).isEqualTo("admin");
    }

    @Test
    void validateToken_returnsTrueForValidToken() {
        Authentication auth = createAuthentication("user1");
        String token = jwtTokenProvider.generateToken(auth);

        boolean valid = jwtTokenProvider.validateToken(token);

        assertThat(valid).isTrue();
    }

    @Test
    void validateToken_returnsFalseForInvalidToken() {
        boolean valid = jwtTokenProvider.validateToken("invalid.token.value");

        assertThat(valid).isFalse();
    }

    @Test
    void validateToken_returnsFalseForNullToken() {
        boolean valid = jwtTokenProvider.validateToken(null);

        assertThat(valid).isFalse();
    }

    @Test
    void validateToken_returnsFalseForExpiredToken() {
        // Create provider with 0 expiration
        JwtTokenProvider expiredProvider = new JwtTokenProvider(SECRET, 0L);
        Authentication auth = createAuthentication("user");
        String token = expiredProvider.generateToken(auth);

        boolean valid = expiredProvider.validateToken(token);

        assertThat(valid).isFalse();
    }

    @Test
    void validateToken_returnsFalseForTokenWithDifferentKey() {
        Authentication auth = createAuthentication("user");
        String token = jwtTokenProvider.generateToken(auth);

        JwtTokenProvider otherProvider = new JwtTokenProvider(
                "another-secret-key-for-testing-256-bits!!!", EXPIRATION);
        boolean valid = otherProvider.validateToken(token);

        assertThat(valid).isFalse();
    }

    private Authentication createAuthentication(String username) {
        User userDetails = new User(username, "password", Collections.emptyList());
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }
}
