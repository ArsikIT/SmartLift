package com.smartlift.integrational;

import com.smartlift.dto.request.LoginRequest;
import com.smartlift.dto.request.RegisterRequest;
import com.smartlift.support.TestRequestFactory;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerIntegrationTest extends BaseIntegrationTest {

    @Test
    void register_createsOrganizationAndUser() throws Exception {
        TestAccount account = testAccount("authtest_admin", "authtest@test.com");
        RegisterRequest request = newRegisterRequest(account, "AuthTestOrg", "MANUFACTURER");

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
        TestAccount existing = testAccount("dup_user", "dup1@test.com");
        registerOrganization(existing, "DupOrg1", "SERVICE");

        RegisterRequest request = TestRequestFactory.registerRequest(
                existing.username(), "other@test.com", "DupOrg2", "SERVICE");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void register_returns409WhenDuplicateEmail() throws Exception {
        TestAccount existing = testAccount("dup_email_user", "dupemail@test.com");
        registerOrganization(existing, "DupEmailOrg", "MANAGEMENT");

        RegisterRequest request = TestRequestFactory.registerRequest(
                "another_user", existing.email(), "AnotherOrg", "MANAGEMENT");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void register_returns409WhenDuplicateOrganizationName() throws Exception {
        TestAccount existing = testAccount("org_dup_user", "orgdup@test.com");
        registerOrganization(existing, "SameOrgName", "MANUFACTURER");

        TestAccount candidate = testAccount("org_dup_user2", "orgdup2@test.com");
        RegisterRequest request = newRegisterRequest(candidate, "SameOrgName", "MANUFACTURER");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void register_returns400WhenInvalidOrgType() throws Exception {
        TestAccount account = testAccount("badtype_user", "badtype@test.com");
        RegisterRequest request = newRegisterRequest(account, "BadTypeOrg", "INVALID_TYPE");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_returns400WhenValidationFails() throws Exception {
        RegisterRequest request = TestRequestFactory.invalidRegisterRequest(
                "", "not-an-email", "short", "", "");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_returnsTokenForValidCredentials() throws Exception {
        TestAccount account = testAccount("login_user", "login@test.com");
        registerOrganization(account, "LoginOrg", "SERVICE");
        LoginRequest request = newLoginRequest(account);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("login_user"));
    }

    @Test
    void login_returns401ForInvalidCredentials() throws Exception {
        LoginRequest request = TestRequestFactory.invalidLoginRequest("nonexistent");

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
        TestAccount account = testAccount("tokentest_user", "tokentest@test.com");
        String token = registerAndLogin(account, "TokenTestOrg", "MANUFACTURER");

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
