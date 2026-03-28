package com.smartlift.integrational;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlift.dto.request.LoginRequest;
import com.smartlift.dto.request.RegisterRequest;
import com.smartlift.dto.response.AuthResponse;
import com.smartlift.dto.response.UserResponse;
import com.smartlift.security.RateLimitFilter;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private RateLimitFilter rateLimitFilter;

    @BeforeEach
    void resetRateLimiter() throws Exception {
        clearField("buckets");
        clearField("lastAccess");
    }

    private void clearField(String fieldName) throws Exception {
        Field field = RateLimitFilter.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        ((ConcurrentHashMap<?, ?>) field.get(rateLimitFilter)).clear();
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

    protected String registerAndLogin(String username, String email,
                                       String password, String orgName,
                                       String orgType) throws Exception {
        registerOrganization(username, email, password, orgName, orgType);
        return login(username, password);
    }
}
