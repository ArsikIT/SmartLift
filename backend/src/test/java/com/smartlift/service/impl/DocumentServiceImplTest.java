package com.smartlift.service.impl;

import com.smartlift.dto.request.DocumentRequest;
import com.smartlift.dto.response.DocumentResponse;
import com.smartlift.exception.ResourceNotFoundException;
import com.smartlift.model.Document;
import com.smartlift.model.Lift;
import com.smartlift.model.Maintenance;
import com.smartlift.model.Organization;
import com.smartlift.model.User;
import com.smartlift.model.enums.LiftStatus;
import com.smartlift.model.enums.MaintenanceStatus;
import com.smartlift.repository.DocumentRepository;
import com.smartlift.repository.LiftRepository;
import com.smartlift.repository.MaintenanceRepository;
import com.smartlift.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceImplTest {

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private LiftRepository liftRepository;
    @Mock
    private MaintenanceRepository maintenanceRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SecurityContextHelper securityHelper;

    @InjectMocks
    private DocumentServiceImpl documentService;

    private final Pageable pageable = PageRequest.of(0, 20);

    @Test
    void getAllDocuments_returnsPage() {
        User user = createUserWithOrg(1L, "admin", 10L);
        when(securityHelper.resolveUser("admin")).thenReturn(user);

        Document doc = createDocument(1L, "file.pdf", "/path/file.pdf");
        Page<Document> page = new PageImpl<>(List.of(doc));
        when(documentRepository.findAllByOrganizationId(10L, pageable)).thenReturn(page);

        Page<DocumentResponse> result = documentService.getAllDocuments("admin", pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getDocumentsByLiftId_returnsFilteredPage() {
        User user = createUserWithOrg(1L, "admin", 10L);
        when(securityHelper.resolveUser("admin")).thenReturn(user);

        Lift lift = createLift(5L);
        when(liftRepository.findById(5L)).thenReturn(Optional.of(lift));

        Document doc = createDocument(1L, "doc.pdf", "/docs/doc.pdf");
        doc.setLift(lift);
        Page<Document> page = new PageImpl<>(List.of(doc));
        when(documentRepository.findAllByLiftId(5L, pageable)).thenReturn(page);

        Page<DocumentResponse> result = documentService.getDocumentsByLiftId("admin", 5L, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getDocumentsByLiftId_throwsWhenLiftNotFound() {
        User user = createUserWithOrg(1L, "admin", 10L);
        when(securityHelper.resolveUser("admin")).thenReturn(user);
        when(liftRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.getDocumentsByLiftId("admin", 99L, pageable))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getDocumentsByMaintenanceId_returnsFilteredPage() {
        User user = createUserWithOrg(1L, "admin", 10L);
        when(securityHelper.resolveUser("admin")).thenReturn(user);

        Lift lift = createLift(5L);
        Maintenance maintenance = new Maintenance();
        maintenance.setId(15L);
        maintenance.setLift(lift);
        when(maintenanceRepository.findById(15L)).thenReturn(Optional.of(maintenance));

        Document doc = createDocument(1L, "report.pdf", "/docs/report.pdf");
        Page<Document> page = new PageImpl<>(List.of(doc));
        when(documentRepository.findAllByMaintenanceId(15L, pageable)).thenReturn(page);

        Page<DocumentResponse> result = documentService.getDocumentsByMaintenanceId("admin", 15L, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getDocumentById_returnsDocument() {
        User user = createUserWithOrg(1L, "admin", 10L);
        when(securityHelper.resolveUser("admin")).thenReturn(user);

        Document doc = createDocument(20L, "test.txt", "/docs/test.txt");
        when(documentRepository.findById(20L)).thenReturn(Optional.of(doc));

        DocumentResponse result = documentService.getDocumentById("admin", 20L);

        assertThat(result.getId()).isEqualTo(20L);
        assertThat(result.getFileName()).isEqualTo("test.txt");
    }

    @Test
    void getDocumentById_throwsWhenNotFound() {
        User user = createUserWithOrg(1L, "admin", 10L);
        when(securityHelper.resolveUser("admin")).thenReturn(user);
        when(documentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.getDocumentById("admin", 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createDocument_createsDocumentWithLift() {
        User user = createUserWithOrg(1L, "admin", 10L);
        when(securityHelper.resolveUser("admin")).thenReturn(user);

        Lift lift = createLift(5L);
        when(liftRepository.findById(5L)).thenReturn(Optional.of(lift));

        DocumentRequest request = new DocumentRequest();
        request.setFileName("newdoc.pdf");
        request.setFilePath("/docs/newdoc.pdf");
        request.setContentType("application/pdf");
        request.setLiftId(5L);

        Document saved = createDocument(30L, "newdoc.pdf", "/docs/newdoc.pdf");
        saved.setLift(lift);
        when(documentRepository.saveAndFlush(any(Document.class))).thenReturn(saved);

        DocumentResponse result = documentService.createDocument("admin", request);

        assertThat(result.getId()).isEqualTo(30L);
        assertThat(result.getLiftId()).isEqualTo(5L);
    }

    @Test
    void createDocument_createsDocumentWithMaintenance() {
        User user = createUserWithOrg(1L, "admin", 10L);
        when(securityHelper.resolveUser("admin")).thenReturn(user);

        Lift lift = createLift(5L);
        Maintenance maintenance = new Maintenance();
        maintenance.setId(15L);
        maintenance.setLift(lift);
        when(maintenanceRepository.findById(15L)).thenReturn(Optional.of(maintenance));

        DocumentRequest request = new DocumentRequest();
        request.setFileName("report.pdf");
        request.setFilePath("/docs/report.pdf");
        request.setMaintenanceId(15L);

        Document saved = createDocument(31L, "report.pdf", "/docs/report.pdf");
        saved.setMaintenance(maintenance);
        when(documentRepository.saveAndFlush(any(Document.class))).thenReturn(saved);

        DocumentResponse result = documentService.createDocument("admin", request);

        assertThat(result.getMaintenanceId()).isEqualTo(15L);
    }

    @Test
    void createDocument_resolvesUploadedByUser() {
        User user = createUserWithOrg(1L, "admin", 10L);
        when(securityHelper.resolveUser("admin")).thenReturn(user);

        User uploader = new User();
        uploader.setId(7L);
        uploader.setUsername("uploader");
        uploader.setEmail("up@test.com");
        when(userRepository.findById(7L)).thenReturn(Optional.of(uploader));

        DocumentRequest request = new DocumentRequest();
        request.setFileName("file.txt");
        request.setFilePath("/docs/file.txt");
        request.setUploadedByUserId(7L);

        Document saved = createDocument(32L, "file.txt", "/docs/file.txt");
        saved.setUploadedBy(uploader);
        when(documentRepository.saveAndFlush(any(Document.class))).thenReturn(saved);

        DocumentResponse result = documentService.createDocument("admin", request);

        assertThat(result.getUploadedBy()).isNotNull();
        assertThat(result.getUploadedBy().getId()).isEqualTo(7L);
    }

    @Test
    void updateDocument_updatesDocumentFields() {
        User user = createUserWithOrg(1L, "admin", 10L);
        when(securityHelper.resolveUser("admin")).thenReturn(user);

        Document existing = createDocument(20L, "old.pdf", "/docs/old.pdf");
        when(documentRepository.findById(20L)).thenReturn(Optional.of(existing));

        DocumentRequest request = new DocumentRequest();
        request.setFileName("updated.pdf");
        request.setFilePath("/docs/updated.pdf");
        request.setContentType("application/pdf");

        Document saved = createDocument(20L, "updated.pdf", "/docs/updated.pdf");
        when(documentRepository.saveAndFlush(any(Document.class))).thenReturn(saved);

        DocumentResponse result = documentService.updateDocument("admin", 20L, request);

        assertThat(result.getFileName()).isEqualTo("updated.pdf");
    }

    @Test
    void deleteDocument_deletesDocument() {
        User user = createUserWithOrg(1L, "admin", 10L);
        when(securityHelper.resolveUser("admin")).thenReturn(user);

        Document doc = createDocument(20L, "del.pdf", "/docs/del.pdf");
        when(documentRepository.findById(20L)).thenReturn(Optional.of(doc));

        documentService.deleteDocument("admin", 20L);

        verify(documentRepository).delete(doc);
        verify(documentRepository).flush();
    }

    private User createUserWithOrg(Long userId, String username, Long orgId) {
        Organization org = new Organization();
        org.setId(orgId);
        User user = new User();
        user.setId(userId);
        user.setUsername(username);
        user.setOrganization(org);
        return user;
    }

    private Lift createLift(Long id) {
        Lift lift = new Lift();
        lift.setId(id);
        lift.setSerialNumber("SN-" + id);
        lift.setStatus(LiftStatus.CREATED);
        return lift;
    }

    private Document createDocument(Long id, String fileName, String filePath) {
        Document doc = new Document();
        doc.setId(id);
        doc.setFileName(fileName);
        doc.setFilePath(filePath);
        return doc;
    }
}
