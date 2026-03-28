package com.smartlift.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlift.dto.request.DocumentRequest;
import com.smartlift.dto.response.DocumentResponse;
import com.smartlift.security.CustomUserDetailsService;
import com.smartlift.security.JwtTokenProvider;
import org.springframework.context.annotation.Import;

import com.smartlift.service.DocumentService;
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

@WebMvcTest(DocumentController.class)
@Import(com.smartlift.config.SecurityConfig.class)
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DocumentService documentService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private DocumentResponse sampleResponse() {
        return DocumentResponse.builder()
                .id(1L)
                .fileName("report.pdf")
                .filePath("/docs/report.pdf")
                .contentType("application/pdf")
                .liftId(5L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getDocuments_returnsAll() throws Exception {
        Page<DocumentResponse> page = new PageImpl<>(List.of(sampleResponse()));
        when(documentService.getAllDocuments(any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].fileName").value("report.pdf"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getDocuments_filtersByLiftId() throws Exception {
        Page<DocumentResponse> page = new PageImpl<>(List.of(sampleResponse()));
        when(documentService.getDocumentsByLiftId(any(), eq(5L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/documents").param("liftId", "5"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getDocuments_filtersByMaintenanceId() throws Exception {
        Page<DocumentResponse> page = new PageImpl<>(List.of(sampleResponse()));
        when(documentService.getDocumentsByMaintenanceId(any(), eq(10L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/documents").param("maintenanceId", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MANAGEMENT")
    void getDocuments_allowsManagementRole() throws Exception {
        Page<DocumentResponse> page = new PageImpl<>(List.of());
        when(documentService.getAllDocuments(any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/documents"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MANUFACTURER")
    void getDocuments_forbidsManufacturerRole() throws Exception {
        mockMvc.perform(get("/api/documents"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getDocuments_returns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/documents"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getDocumentById_returns200() throws Exception {
        when(documentService.getDocumentById(any(), eq(1L))).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/documents/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createDocument_returns201() throws Exception {
        when(documentService.createDocument(any(), any(DocumentRequest.class))).thenReturn(sampleResponse());

        DocumentRequest request = new DocumentRequest();
        request.setFileName("new.pdf");
        request.setFilePath("/docs/new.pdf");

        mockMvc.perform(post("/api/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/documents/1"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createDocument_returns400WhenValidationFails() throws Exception {
        DocumentRequest request = new DocumentRequest();
        request.setFileName("");
        request.setFilePath("");

        mockMvc.perform(post("/api/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "SERVICE")
    void updateDocument_returns200() throws Exception {
        when(documentService.updateDocument(any(), eq(1L), any(DocumentRequest.class)))
                .thenReturn(sampleResponse());

        DocumentRequest request = new DocumentRequest();
        request.setFileName("updated.pdf");
        request.setFilePath("/docs/updated.pdf");

        mockMvc.perform(put("/api/documents/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MANAGEMENT")
    void updateDocument_forbidsManagementRole() throws Exception {
        DocumentRequest request = new DocumentRequest();
        request.setFileName("file.pdf");
        request.setFilePath("/docs/file.pdf");

        mockMvc.perform(put("/api/documents/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteDocument_returns204() throws Exception {
        doNothing().when(documentService).deleteDocument(any(), eq(1L));

        mockMvc.perform(delete("/api/documents/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "MANAGEMENT")
    void deleteDocument_forbidsManagementRole() throws Exception {
        mockMvc.perform(delete("/api/documents/1"))
                .andExpect(status().isForbidden());
    }
}
