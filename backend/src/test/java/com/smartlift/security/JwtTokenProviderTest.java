package com.smartlift.security;

import com.smartlift.support.TestPasswords;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    private static final String SIGNING_KEY =
            String.join("-", "jwt", "signing", "key", "for", "tests", "with", "enough", "length", "256");
    private static final String ALTERNATE_SIGNING_KEY =
            String.join("-", "jwt", "alternate", "key", "for", "tests", "with", "enough", "length", "256");
    private static final long EXPIRATION = 3600000L; // 1 hour

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(SIGNING_KEY, EXPIRATION);
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
        JwtTokenProvider expiredProvider = new JwtTokenProvider(SIGNING_KEY, 0L);
        Authentication auth = createAuthentication("user");
        String token = expiredProvider.generateToken(auth);

        boolean valid = expiredProvider.validateToken(token);

        assertThat(valid).isFalse();
    }

    @Test
    void validateToken_returnsFalseForTokenWithDifferentKey() {
        Authentication auth = createAuthentication("user");
        String token = jwtTokenProvider.generateToken(auth);

        JwtTokenProvider otherProvider = new JwtTokenProvider(ALTERNATE_SIGNING_KEY, EXPIRATION);
        boolean valid = otherProvider.validateToken(token);

        assertThat(valid).isFalse();
    }

    private Authentication createAuthentication(String username) {
        User userDetails = new User(username, TestPasswords.BASIC, Collections.emptyList());
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }
}
