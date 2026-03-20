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
import java.util.List;
import lombok.RequiredArgsConstructor;
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

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponse> getAllDocuments() {
        return documentRepository.findAll().stream()
                .map(SmartLiftMapper::toDocumentResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocumentsByLiftId(Long liftId) {
        if (!liftRepository.existsById(liftId)) {
            throw new ResourceNotFoundException("Lift not found: " + liftId);
        }
        return documentRepository.findAllByLiftId(liftId).stream()
                .map(SmartLiftMapper::toDocumentResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocumentsByMaintenanceId(Long maintenanceId) {
        if (!maintenanceRepository.existsById(maintenanceId)) {
            throw new ResourceNotFoundException("Maintenance not found: " + maintenanceId);
        }
        return documentRepository.findAllByMaintenanceId(maintenanceId).stream()
                .map(SmartLiftMapper::toDocumentResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentResponse getDocumentById(Long id) {
        return SmartLiftMapper.toDocumentResponse(getOrThrow(id));
    }

    @Override
    public DocumentResponse createDocument(DocumentRequest request) {
        Document doc = new Document();
        applyRequest(doc, request);
        Document saved = documentRepository.saveAndFlush(doc);
        return SmartLiftMapper.toDocumentResponse(saved);
    }

    @Override
    public DocumentResponse updateDocument(Long id, DocumentRequest request) {
        Document doc = getOrThrow(id);
        applyRequest(doc, request);
        Document saved = documentRepository.saveAndFlush(doc);
        return SmartLiftMapper.toDocumentResponse(saved);
    }

    @Override
    public void deleteDocument(Long id) {
        Document doc = getOrThrow(id);
        documentRepository.delete(doc);
        documentRepository.flush();
    }

    private void applyRequest(Document doc, DocumentRequest request) {
        doc.setFileName(request.getFileName().trim());
        doc.setFilePath(request.getFilePath().trim());
        doc.setContentType(request.getContentType());
        doc.setLift(resolveLift(request.getLiftId()));
        doc.setMaintenance(resolveMaintenance(request.getMaintenanceId()));
        doc.setUploadedBy(resolveUser(request.getUploadedByUserId()));
    }

    private Lift resolveLift(Long liftId) {
        if (liftId == null) {
            return null;
        }
        return liftRepository.findById(liftId)
                .orElseThrow(() -> new ResourceNotFoundException("Lift not found: " + liftId));
    }

    private Maintenance resolveMaintenance(Long maintenanceId) {
        if (maintenanceId == null) {
            return null;
        }
        return maintenanceRepository.findById(maintenanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance not found: " + maintenanceId));
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
