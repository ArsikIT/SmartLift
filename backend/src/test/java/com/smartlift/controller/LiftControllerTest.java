package com.smartlift.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlift.dto.request.LiftRequest;
import com.smartlift.dto.response.LiftResponse;
import com.smartlift.exception.ConflictException;
import com.smartlift.exception.ResourceNotFoundException;
import com.smartlift.model.enums.LiftStatus;
import com.smartlift.security.CustomUserDetailsService;
import com.smartlift.security.JwtTokenProvider;
import com.smartlift.service.LiftService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LiftController.class)
@Import(com.smartlift.config.SecurityConfig.class)
class LiftControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LiftService liftService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private LiftResponse sampleLiftResponse() {
        return LiftResponse.builder()
                .id(1L)
                .serialNumber("SN-001")
                .model("ModelX")
                .manufacturer("Acme")
                .status(LiftStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllLifts_returns200() throws Exception {
        Page<LiftResponse> page = new PageImpl<>(List.of(sampleLiftResponse()));
        when(liftService.getAllLifts(any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/lifts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].serialNumber").value("SN-001"));
    }

    @Test
    @WithMockUser(roles = "SERVICE")
    void getAllLifts_allowsServiceRole() throws Exception {
        Page<LiftResponse> page = new PageImpl<>(List.of(sampleLiftResponse()));
        when(liftService.getAllLifts(any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/lifts"))
                .andExpect(status().isOk());
    }

    @Test
    void getAllLifts_returns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/lifts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getLiftById_returns200() throws Exception {
        when(liftService.getLiftById(any(), eq(1L))).thenReturn(sampleLiftResponse());

        mockMvc.perform(get("/api/lifts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.serialNumber").value("SN-001"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getLiftById_returns404WhenNotFound() throws Exception {
        when(liftService.getLiftById(any(), eq(99L)))
                .thenThrow(new ResourceNotFoundException("Lift not found: 99"));

        mockMvc.perform(get("/api/lifts/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createLift_returns201() throws Exception {
        when(liftService.createLift(any(), any(LiftRequest.class))).thenReturn(sampleLiftResponse());

        LiftRequest request = new LiftRequest();
        request.setSerialNumber("SN-001");
        request.setModel("ModelX");

        mockMvc.perform(post("/api/lifts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/lifts/1"))
                .andExpect(jsonPath("$.serialNumber").value("SN-001"));
    }

    @Test
    @WithMockUser(roles = "MANUFACTURER")
    void createLift_allowsManufacturerRole() throws Exception {
        when(liftService.createLift(any(), any(LiftRequest.class))).thenReturn(sampleLiftResponse());

        LiftRequest request = new LiftRequest();
        request.setSerialNumber("SN-001");
        request.setModel("ModelX");

        mockMvc.perform(post("/api/lifts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "SERVICE")
    void createLift_forbidsServiceRole() throws Exception {
        LiftRequest request = new LiftRequest();
        request.setSerialNumber("SN-001");
        request.setModel("ModelX");

        mockMvc.perform(post("/api/lifts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGEMENT")
    void createLift_forbidsManagementRole() throws Exception {
        LiftRequest request = new LiftRequest();
        request.setSerialNumber("SN-001");
        request.setModel("ModelX");

        mockMvc.perform(post("/api/lifts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createLift_returns400WhenValidationFails() throws Exception {
        LiftRequest request = new LiftRequest();
        request.setSerialNumber("");
        request.setModel("");

        mockMvc.perform(post("/api/lifts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createLift_returns409WhenDuplicate() throws Exception {
        when(liftService.createLift(any(), any(LiftRequest.class)))
                .thenThrow(new ConflictException("Lift serial number already exists"));

        LiftRequest request = new LiftRequest();
        request.setSerialNumber("SN-DUP");
        request.setModel("Model");

        mockMvc.perform(post("/api/lifts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateLift_returns200() throws Exception {
        when(liftService.updateLift(any(), eq(1L), any(LiftRequest.class))).thenReturn(sampleLiftResponse());

        LiftRequest request = new LiftRequest();
        request.setSerialNumber("SN-001");
        request.setModel("UpdatedModel");

        mockMvc.perform(put("/api/lifts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteLift_returns204() throws Exception {
        doNothing().when(liftService).deleteLift(any(), eq(1L));

        mockMvc.perform(delete("/api/lifts/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "SERVICE")
    void deleteLift_forbidsServiceRole() throws Exception {
        mockMvc.perform(delete("/api/lifts/1"))
                .andExpect(status().isForbidden());
    }
}
