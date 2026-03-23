package com.smartlift.integrational;

import com.smartlift.dto.request.LoginRequest;
import com.smartlift.dto.request.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerIntegrationTest extends BaseIntegrationTest {

    @Test
    void register_createsOrganizationAndUser() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("authtest_admin");
        request.setEmail("authtest@test.com");
        request.setPassword("password123");
        request.setOrganizationName("AuthTestOrg");
        request.setOrganizationType("MANUFACTURER");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.username").value("authtest_admin"))
                .andExpect(jsonPath("$.email").value("authtest@test.com"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.organization.name").value("AuthTestOrg"))
                .andExpect(jsonPath("$.organization.type").value("MANUFACTURER"))
                .andExpect(jsonPath("$.roles").isArray());
    }

    @Test
    void register_returns409WhenDuplicateUsername() throws Exception {
        registerOrganization("dup_user", "dup1@test.com", "password123", "DupOrg1", "SERVICE");

        RegisterRequest request = new RegisterRequest();
        request.setUsername("dup_user");
        request.setEmail("other@test.com");
        request.setPassword("password123");
        request.setOrganizationName("DupOrg2");
        request.setOrganizationType("SERVICE");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void register_returns409WhenDuplicateEmail() throws Exception {
        registerOrganization("dup_email_user", "dupemail@test.com", "password123", "DupEmailOrg", "MANAGEMENT");

        RegisterRequest request = new RegisterRequest();
        request.setUsername("another_user");
        request.setEmail("dupemail@test.com");
        request.setPassword("password123");
        request.setOrganizationName("AnotherOrg");
        request.setOrganizationType("MANAGEMENT");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void register_returns409WhenDuplicateOrganizationName() throws Exception {
        registerOrganization("org_dup_user", "orgdup@test.com", "password123", "SameOrgName", "MANUFACTURER");

        RegisterRequest request = new RegisterRequest();
        request.setUsername("org_dup_user2");
        request.setEmail("orgdup2@test.com");
        request.setPassword("password123");
        request.setOrganizationName("SameOrgName");
        request.setOrganizationType("MANUFACTURER");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void register_returns400WhenInvalidOrgType() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("badtype_user");
        request.setEmail("badtype@test.com");
        request.setPassword("password123");
        request.setOrganizationName("BadTypeOrg");
        request.setOrganizationType("INVALID_TYPE");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_returns400WhenValidationFails() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("");
        request.setEmail("not-an-email");
        request.setPassword("short");
        request.setOrganizationName("");
        request.setOrganizationType("");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_returnsTokenForValidCredentials() throws Exception {
        registerOrganization("login_user", "login@test.com", "password123", "LoginOrg", "SERVICE");

        LoginRequest request = new LoginRequest();
        request.setUsername("login_user");
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("login_user"));
    }

    @Test
    void login_returns401ForInvalidCredentials() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("nonexistent");
        request.setPassword("wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_returns401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_returns200WithValidToken() throws Exception {
        String token = registerAndLogin("tokentest_user", "tokentest@test.com",
                "password123", "TokenTestOrg", "MANUFACTURER");

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
