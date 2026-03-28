package com.smartlift.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlift.dto.request.LoginRequest;
import com.smartlift.dto.request.RegisterRequest;
import com.smartlift.dto.response.AuthResponse;
import com.smartlift.dto.response.OrganizationSummaryResponse;
import com.smartlift.dto.response.UserResponse;
import com.smartlift.exception.ConflictException;
import com.smartlift.model.enums.OrganizationType;
import com.smartlift.security.CustomUserDetailsService;
import com.smartlift.security.JwtTokenProvider;
import com.smartlift.service.AuthService;
import com.smartlift.service.RegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(com.smartlift.config.SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private RegistrationService registrationService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void login_returns200WithToken() throws Exception {
        AuthResponse authResponse = AuthResponse.builder()
                .token("jwt-token")
                .username("admin")
                .build();
        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("password");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.username").value("admin"));
    }

    @Test
    void login_returns400WhenUsernameBlank() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("");
        request.setPassword("password");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_returns201WithUserResponse() throws Exception {
        UserResponse userResponse = UserResponse.builder()
                .id(1L)
                .username("admin")
                .email("admin@test.com")
                .enabled(true)
                .organization(OrganizationSummaryResponse.builder()
                        .id(1L).name("TestOrg").type(OrganizationType.MANUFACTURER).build())
                .roles(Set.of("ADMIN", "MANUFACTURER"))
                .build();
        when(registrationService.register(any(RegisterRequest.class))).thenReturn(userResponse);

        RegisterRequest request = new RegisterRequest();
        request.setUsername("admin");
        request.setEmail("admin@test.com");
        request.setPassword("password123");
        request.setOrganizationName("TestOrg");
        request.setOrganizationType("MANUFACTURER");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/users/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("admin"));
    }

    @Test
    void register_returns409WhenConflict() throws Exception {
        when(registrationService.register(any(RegisterRequest.class)))
                .thenThrow(new ConflictException("Username already exists"));

        RegisterRequest request = new RegisterRequest();
        request.setUsername("existing");
        request.setEmail("ex@test.com");
        request.setPassword("password123");
        request.setOrganizationName("Org");
        request.setOrganizationType("SERVICE");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void register_returns400WhenValidationFails() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("");
        request.setEmail("invalid");
        request.setPassword("short");
        request.setOrganizationName("");
        request.setOrganizationType("");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
