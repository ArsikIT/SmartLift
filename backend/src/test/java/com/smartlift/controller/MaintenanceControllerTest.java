package com.smartlift.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlift.dto.request.MaintenanceRequest;
import com.smartlift.dto.response.MaintenanceResponse;
import com.smartlift.model.enums.MaintenanceStatus;
import com.smartlift.security.CustomUserDetailsService;
import org.springframework.context.annotation.Import;
import com.smartlift.security.JwtTokenProvider;
import com.smartlift.service.MaintenanceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MaintenanceController.class)
@Import(com.smartlift.config.SecurityConfig.class)
class MaintenanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MaintenanceService maintenanceService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private MaintenanceResponse sampleResponse() {
        return MaintenanceResponse.builder()
                .id(1L)
                .liftId(5L)
                .liftSerialNumber("SN-005")
                .title("Annual check")
                .description("Yearly inspection")
                .status(MaintenanceStatus.PENDING)
                .requestedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getMaintenances_returnsAll() throws Exception {
        Page<MaintenanceResponse> page = new PageImpl<>(List.of(sampleResponse()));
        when(maintenanceService.getAllMaintenances(any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/maintenances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Annual check"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getMaintenances_filtersByLiftId() throws Exception {
        Page<MaintenanceResponse> page = new PageImpl<>(List.of(sampleResponse()));
        when(maintenanceService.getMaintenancesByLiftId(any(), eq(5L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/maintenances").param("liftId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].liftId").value(5));
    }

    @Test
    @WithMockUser(roles = "MANAGEMENT")
    void getMaintenances_allowsManagementRole() throws Exception {
        Page<MaintenanceResponse> page = new PageImpl<>(List.of());
        when(maintenanceService.getAllMaintenances(any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/maintenances"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MANUFACTURER")
    void getMaintenances_forbidsManufacturerRole() throws Exception {
        mockMvc.perform(get("/api/maintenances"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMaintenances_returns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/maintenances"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getMaintenanceById_returns200() throws Exception {
        when(maintenanceService.getMaintenanceById(any(), eq(1L))).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/maintenances/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createMaintenance_returns201() throws Exception {
        when(maintenanceService.createMaintenance(any(), any(MaintenanceRequest.class)))
                .thenReturn(sampleResponse());

        MaintenanceRequest request = new MaintenanceRequest();
        request.setLiftId(5L);
        request.setTitle("Annual check");

        mockMvc.perform(post("/api/maintenances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/maintenances/1"));
    }

    @Test
    @WithMockUser(roles = "SERVICE")
    void createMaintenance_allowsServiceRole() throws Exception {
        when(maintenanceService.createMaintenance(any(), any(MaintenanceRequest.class)))
                .thenReturn(sampleResponse());

        MaintenanceRequest request = new MaintenanceRequest();
        request.setLiftId(5L);
        request.setTitle("Repair");

        mockMvc.perform(post("/api/maintenances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createMaintenance_returns400WhenValidationFails() throws Exception {
        MaintenanceRequest request = new MaintenanceRequest();

        mockMvc.perform(post("/api/maintenances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateMaintenance_returns200() throws Exception {
        when(maintenanceService.updateMaintenance(any(), eq(1L), any(MaintenanceRequest.class)))
                .thenReturn(sampleResponse());

        MaintenanceRequest request = new MaintenanceRequest();
        request.setLiftId(5L);
        request.setTitle("Updated check");

        mockMvc.perform(put("/api/maintenances/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MANAGEMENT")
    void updateMaintenance_forbidsManagementRole() throws Exception {
        MaintenanceRequest request = new MaintenanceRequest();
        request.setLiftId(5L);
        request.setTitle("Title");

        mockMvc.perform(put("/api/maintenances/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteMaintenance_returns204() throws Exception {
        doNothing().when(maintenanceService).deleteMaintenance(any(), eq(1L));

        mockMvc.perform(delete("/api/maintenances/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "MANAGEMENT")
    void deleteMaintenance_forbidsManagementRole() throws Exception {
        mockMvc.perform(delete("/api/maintenances/1"))
                .andExpect(status().isForbidden());
    }
}
