package com.smartlift.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlift.dto.request.LiftEventRequest;
import com.smartlift.dto.response.LiftEventResponse;
import com.smartlift.model.enums.LiftEventType;
import com.smartlift.security.CustomUserDetailsService;
import com.smartlift.security.JwtTokenProvider;
import com.smartlift.service.LiftEventService;
import org.springframework.context.annotation.Import;

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

@WebMvcTest(LiftEventController.class)
@Import(com.smartlift.config.SecurityConfig.class)
class LiftEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LiftEventService liftEventService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private LiftEventResponse sampleEventResponse() {
        return LiftEventResponse.builder()
                .id(1L)
                .liftId(5L)
                .liftSerialNumber("SN-005")
                .type(LiftEventType.FAULT)
                .eventAt(LocalDateTime.now())
                .description("Motor failure")
                .build();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getEvents_returnsAllEvents() throws Exception {
        Page<LiftEventResponse> page = new PageImpl<>(List.of(sampleEventResponse()));
        when(liftEventService.getAllEvents(any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("FAULT"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getEvents_filtersByLiftId() throws Exception {
        Page<LiftEventResponse> page = new PageImpl<>(List.of(sampleEventResponse()));
        when(liftEventService.getEventsByLiftId(any(), eq(5L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/events").param("liftId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].liftId").value(5));
    }

    @Test
    @WithMockUser(roles = "SERVICE")
    void getEvents_allowsServiceRole() throws Exception {
        Page<LiftEventResponse> page = new PageImpl<>(List.of());
        when(liftEventService.getAllEvents(any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MANAGEMENT")
    void getEvents_forbidsManagementRole() throws Exception {
        mockMvc.perform(get("/api/events"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getEvents_returns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/events"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getEventById_returns200() throws Exception {
        when(liftEventService.getEventById(any(), eq(1L))).thenReturn(sampleEventResponse());

        mockMvc.perform(get("/api/events/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createEvent_returns201() throws Exception {
        LiftEventResponse response = sampleEventResponse();
        when(liftEventService.createEvent(any(), any(LiftEventRequest.class))).thenReturn(response);

        LiftEventRequest request = new LiftEventRequest();
        request.setLiftId(5L);
        request.setType(LiftEventType.FAULT);
        request.setDescription("Motor failure");

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/events/1"));
    }

    @Test
    @WithMockUser(roles = "MANUFACTURER")
    void createEvent_forbidsManufacturerRole() throws Exception {
        LiftEventRequest request = new LiftEventRequest();
        request.setLiftId(5L);
        request.setType(LiftEventType.FAULT);
        request.setDescription("Test");

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createEvent_returns400WhenValidationFails() throws Exception {
        LiftEventRequest request = new LiftEventRequest();

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateEvent_returns200() throws Exception {
        when(liftEventService.updateEvent(any(), eq(1L), any(LiftEventRequest.class)))
                .thenReturn(sampleEventResponse());

        LiftEventRequest request = new LiftEventRequest();
        request.setLiftId(5L);
        request.setType(LiftEventType.REPAIR);
        request.setDescription("Repaired");

        mockMvc.perform(put("/api/events/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "SERVICE")
    void deleteEvent_returns204() throws Exception {
        doNothing().when(liftEventService).deleteEvent(any(), eq(1L));

        mockMvc.perform(delete("/api/events/1"))
                .andExpect(status().isNoContent());
    }
}
