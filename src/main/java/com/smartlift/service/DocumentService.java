package com.smartlift.service;

import com.smartlift.dto.request.DocumentRequest;
import com.smartlift.dto.response.DocumentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DocumentService {

    Page<DocumentResponse> getAllDocuments(String currentUsername, Pageable pageable);

    Page<DocumentResponse> getDocumentsByLiftId(String currentUsername, Long liftId, Pageable pageable);

    Page<DocumentResponse> getDocumentsByMaintenanceId(String currentUsername, Long maintenanceId, Pageable pageable);

    DocumentResponse getDocumentById(String currentUsername, Long id);

    DocumentResponse createDocument(String currentUsername, DocumentRequest request);

    DocumentResponse updateDocument(String currentUsername, Long id, DocumentRequest request);

    void deleteDocument(String currentUsername, Long id);
}
