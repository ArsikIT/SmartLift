package com.smartlift.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RateLimitFilterTest {

    private RateLimitFilter rateLimitFilter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        rateLimitFilter = new RateLimitFilter();
        filterChain = mock(FilterChain.class);
    }

    @Test
    void doFilterInternal_allowsNonAuthRequests() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/lifts");
        request.setRequestURI("/api/lifts");
        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void doFilterInternal_allowsAuthRequestWithinLimit() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRequestURI("/api/auth/login");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getHeader("X-Rate-Limit-Remaining")).isNotNull();
    }

    @Test
    void doFilterInternal_blocksAfterExceedingRateLimit() throws ServletException, IOException {
        String ip = "10.0.0.1";

        // Exhaust 5 tokens
        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
            request.setRequestURI("/api/auth/login");
            request.setRemoteAddr(ip);
            MockHttpServletResponse response = new MockHttpServletResponse();
            rateLimitFilter.doFilterInternal(request, response, filterChain);
        }

        // 6th request should be blocked
        MockHttpServletRequest blockedRequest = new MockHttpServletRequest("POST", "/api/auth/login");
        blockedRequest.setRequestURI("/api/auth/login");
        blockedRequest.setRemoteAddr(ip);
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();

        rateLimitFilter.doFilterInternal(blockedRequest, blockedResponse, filterChain);

        assertThat(blockedResponse.getStatus()).isEqualTo(429);
        assertThat(blockedResponse.getHeader("Retry-After")).isNotNull();
        assertThat(blockedResponse.getContentAsString()).contains("Too many requests");
    }

    @Test
    void doFilterInternal_rateLimitsPerIp() throws ServletException, IOException {
        // Exhaust IP1
        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
            request.setRequestURI("/api/auth/login");
            request.setRemoteAddr("192.168.1.1");
            rateLimitFilter.doFilterInternal(request, new MockHttpServletResponse(), filterChain);
        }

        // IP2 should still be allowed
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRequestURI("/api/auth/login");
        request.setRemoteAddr("192.168.1.2");
        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isNotEqualTo(429);
    }

    @Test
    void doFilterInternal_registerPathIsAlsoRateLimited() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/register");
        request.setRequestURI("/api/auth/register");
        request.setRemoteAddr("172.16.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getHeader("X-Rate-Limit-Remaining")).isNotNull();
    }

    @Test
    void cleanup_removesExpiredBuckets() {
        // Just verify cleanup runs without exceptions
        rateLimitFilter.cleanup();
    }
}
