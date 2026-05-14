package com.smartlift.integrational;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlift.config.AsyncTestConfig;
import com.smartlift.dto.request.LoginRequest;
import com.smartlift.dto.request.RegisterRequest;
import com.smartlift.dto.request.UserRequest;
import com.smartlift.dto.response.AuthResponse;
import com.smartlift.dto.response.UserResponse;
import com.smartlift.security.RateLimitFilter;
import com.smartlift.support.TestRequestFactory;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AsyncTestConfig.class)
public abstract class BaseIntegrationTest {

    protected record TestAccount(String username, String email) {
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RateLimitFilter rateLimitFilter;

    @BeforeEach
    void resetTestState() throws Exception {
        resetDatabase();
        clearField("buckets");
        clearField("lastAccess");
    }

    private void resetDatabase() {
        jdbcTemplate.execute("""
                TRUNCATE TABLE notifications, documents, maintenances, lift_events, lifts, user_roles, users, roles, organizations
                RESTART IDENTITY CASCADE
                """);
        jdbcTemplate.update("""
                INSERT INTO roles (name, created_at, updated_at) VALUES
                    ('ADMIN', NOW(), NOW()),
                    ('SERVICE', NOW(), NOW()),
                    ('MANAGEMENT', NOW(), NOW()),
                    ('MANUFACTURER', NOW(), NOW())
                """);
    }

    private void clearField(String fieldName) throws Exception {
        Field field = RateLimitFilter.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        ((ConcurrentHashMap<?, ?>) field.get(rateLimitFilter)).clear();
    }

    protected TestAccount testAccount(String username, String email) {
        return new TestAccount(username, email);
    }

    protected RegisterRequest newRegisterRequest(TestAccount account, String orgName, String orgType) {
        return TestRequestFactory.registerRequest(account.username(), account.email(), orgName, orgType);
    }

    protected LoginRequest newLoginRequest(TestAccount account) {
        return TestRequestFactory.loginRequest(account.username());
    }

    protected UserRequest newUserRequest(TestAccount account) {
        return TestRequestFactory.userRequest(account.username(), account.email());
    }

    protected UserRequest newUpdatedUserRequest(TestAccount account) {
        return TestRequestFactory.updatedUserRequest(account.username(), account.email());
    }

    protected UserResponse registerOrganization(String username, String email,
                                                 String password, String orgName,
                                                 String orgType) throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setEmail(email);
        request.setPassword(password);
        request.setOrganizationName(orgName);
        request.setOrganizationType(orgType);

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readValue(result.getResponse().getContentAsString(), UserResponse.class);
    }

    protected UserResponse registerOrganization(TestAccount account, String orgName,
                                                String orgType) throws Exception {
        RegisterRequest request = newRegisterRequest(account, orgName, orgType);

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readValue(result.getResponse().getContentAsString(), UserResponse.class);
    }

    protected String login(String username, String password) throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse authResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthResponse.class);
        return authResponse.getToken();
    }

    protected String login(TestAccount account) throws Exception {
        LoginRequest request = newLoginRequest(account);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse authResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthResponse.class);
        return authResponse.getToken();
    }

    protected String registerAndLogin(String username, String email,
                                       String password, String orgName,
                                       String orgType) throws Exception {
        registerOrganization(username, email, password, orgName, orgType);
        return login(username, password);
    }

    protected String registerAndLogin(TestAccount account, String orgName,
                                      String orgType) throws Exception {
        registerOrganization(account, orgName, orgType);
        return login(account);
    }
}
