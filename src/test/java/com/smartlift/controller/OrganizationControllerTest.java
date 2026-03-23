package com.smartlift.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlift.dto.request.OrganizationRequest;
import com.smartlift.dto.response.OrganizationResponse;
import com.smartlift.exception.ConflictException;
import com.smartlift.model.enums.OrganizationType;
import com.smartlift.security.CustomUserDetailsService;
import com.smartlift.security.JwtTokenProvider;
import com.smartlift.service.OrganizationService;
import org.springframework.context.annotation.Import;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrganizationController.class)
@Import(com.smartlift.config.SecurityConfig.class)
class OrganizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrganizationService organizationService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private OrganizationResponse sampleResponse() {
        return OrganizationResponse.builder()
                .id(1L)
                .name("TestOrg")
                .type(OrganizationType.MANUFACTURER)
                .address("123 Main St")
                .contactEmail("contact@test.com")
                .contactPhone("+1234567890")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getMyOrganization_returns200() throws Exception {
        when(organizationService.getMyOrganization(any())).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/organizations/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("TestOrg"))
                .andExpect(jsonPath("$.type").value("MANUFACTURER"));
    }

    @Test
    @WithMockUser(roles = "SERVICE")
    void getMyOrganization_forbidsNonAdminRole() throws Exception {
        mockMvc.perform(get("/api/organizations/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGEMENT")
    void getMyOrganization_forbidsManagementRole() throws Exception {
        mockMvc.perform(get("/api/organizations/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMyOrganization_returns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/organizations/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateMyOrganization_returns200() throws Exception {
        when(organizationService.updateMyOrganization(any(), any(OrganizationRequest.class)))
                .thenReturn(sampleResponse());

        OrganizationRequest request = new OrganizationRequest();
        request.setName("UpdatedOrg");
        request.setType(OrganizationType.MANUFACTURER);
        request.setAddress("456 New St");

        mockMvc.perform(put("/api/organizations/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("TestOrg"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateMyOrganization_returns409WhenNameTaken() throws Exception {
        when(organizationService.updateMyOrganization(any(), any(OrganizationRequest.class)))
                .thenThrow(new ConflictException("Organization name already exists"));

        OrganizationRequest request = new OrganizationRequest();
        request.setName("TakenName");
        request.setType(OrganizationType.SERVICE);

        mockMvc.perform(put("/api/organizations/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateMyOrganization_returns400WhenValidationFails() throws Exception {
        OrganizationRequest request = new OrganizationRequest();
        request.setName("");
        request.setType(null);

        mockMvc.perform(put("/api/organizations/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "SERVICE")
    void updateMyOrganization_forbidsNonAdminRole() throws Exception {
        OrganizationRequest request = new OrganizationRequest();
        request.setName("Org");
        request.setType(OrganizationType.SERVICE);

        mockMvc.perform(put("/api/organizations/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
