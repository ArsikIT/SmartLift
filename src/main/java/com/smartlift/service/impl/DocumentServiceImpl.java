package com.smartlift.service.impl;

import com.smartlift.dto.request.DocumentRequest;
import com.smartlift.dto.response.DocumentResponse;
import com.smartlift.exception.ResourceNotFoundException;
import com.smartlift.mapper.SmartLiftMapper;
import com.smartlift.model.Document;
import com.smartlift.model.Lift;
import com.smartlift.model.Maintenance;
import com.smartlift.model.User;
import com.smartlift.repository.DocumentRepository;
import com.smartlift.repository.LiftRepository;
import com.smartlift.repository.MaintenanceRepository;
import com.smartlift.repository.UserRepository;
import com.smartlift.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final LiftRepository liftRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final UserRepository userRepository;
    private final SecurityContextHelper securityHelper;

    @Override
    @Transactional(readOnly = true)
    public Page<DocumentResponse> getAllDocuments(String currentUsername, Pageable pageable) {
        User user = securityHelper.resolveUser(currentUsername);
        return documentRepository.findAllByOrganizationId(user.getOrganization().getId(), pageable)
                .map(SmartLiftMapper::toDocumentResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DocumentResponse> getDocumentsByLiftId(String currentUsername, Long liftId, Pageable pageable) {
        User user = securityHelper.resolveUser(currentUsername);
        Lift lift = liftRepository.findById(liftId)
                .orElseThrow(() -> new ResourceNotFoundException("Lift not found: " + liftId));
        securityHelper.checkLiftBelongsToOrg(lift, user.getOrganization().getId());
        return documentRepository.findAllByLiftId(liftId, pageable)
                .map(SmartLiftMapper::toDocumentResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DocumentResponse> getDocumentsByMaintenanceId(String currentUsername, Long maintenanceId, Pageable pageable) {
        User user = securityHelper.resolveUser(currentUsername);
        Maintenance maintenance = maintenanceRepository.findById(maintenanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance not found: " + maintenanceId));
        securityHelper.checkLiftBelongsToOrg(maintenance.getLift(), user.getOrganization().getId());
        return documentRepository.findAllByMaintenanceId(maintenanceId, pageable)
                .map(SmartLiftMapper::toDocumentResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentResponse getDocumentById(String currentUsername, Long id) {
        User user = securityHelper.resolveUser(currentUsername);
        Document doc = getOrThrow(id);
        checkDocumentAccess(doc, user.getOrganization().getId());
        return SmartLiftMapper.toDocumentResponse(doc);
    }

    @Override
    public DocumentResponse createDocument(String currentUsername, DocumentRequest request) {
        User user = securityHelper.resolveUser(currentUsername);
        Document doc = new Document();
        applyRequest(doc, request, user.getOrganization().getId());
        Document saved = documentRepository.saveAndFlush(doc);
        return SmartLiftMapper.toDocumentResponse(saved);
    }

    @Override
    public DocumentResponse updateDocument(String currentUsername, Long id, DocumentRequest request) {
        User user = securityHelper.resolveUser(currentUsername);
        Document doc = getOrThrow(id);
        checkDocumentAccess(doc, user.getOrganization().getId());
        applyRequest(doc, request, user.getOrganization().getId());
        Document saved = documentRepository.saveAndFlush(doc);
        return SmartLiftMapper.toDocumentResponse(saved);
    }

    @Override
    public void deleteDocument(String currentUsername, Long id) {
        User user = securityHelper.resolveUser(currentUsername);
        Document doc = getOrThrow(id);
        checkDocumentAccess(doc, user.getOrganization().getId());
        documentRepository.delete(doc);
        documentRepository.flush();
    }

    private void applyRequest(Document doc, DocumentRequest request, Long orgId) {
        doc.setFileName(request.getFileName().trim());
        doc.setFilePath(request.getFilePath().trim());
        doc.setContentType(request.getContentType());

        if (request.getLiftId() != null) {
            Lift lift = liftRepository.findById(request.getLiftId())
                    .orElseThrow(() -> new ResourceNotFoundException("Lift not found: " + request.getLiftId()));
            securityHelper.checkLiftBelongsToOrg(lift, orgId);
            doc.setLift(lift);
        } else {
            doc.setLift(null);
        }

        if (request.getMaintenanceId() != null) {
            Maintenance maintenance = maintenanceRepository.findById(request.getMaintenanceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Maintenance not found: " + request.getMaintenanceId()));
            securityHelper.checkLiftBelongsToOrg(maintenance.getLift(), orgId);
            doc.setMaintenance(maintenance);
        } else {
            doc.setMaintenance(null);
        }

        doc.setUploadedBy(resolveUser(request.getUploadedByUserId()));
    }

    private void checkDocumentAccess(Document doc, Long orgId) {
        if (doc.getLift() != null) {
            securityHelper.checkLiftBelongsToOrg(doc.getLift(), orgId);
        } else if (doc.getMaintenance() != null) {
            securityHelper.checkLiftBelongsToOrg(doc.getMaintenance().getLift(), orgId);
        }
    }

    private User resolveUser(Long userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    private Document getOrThrow(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + id));
    }
}
