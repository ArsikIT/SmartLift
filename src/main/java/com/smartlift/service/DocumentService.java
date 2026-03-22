package com.smartlift.service;

import com.smartlift.dto.request.DocumentRequest;
import com.smartlift.dto.response.DocumentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DocumentService {

    Page<DocumentResponse> getAllDocuments(Pageable pageable);

    Page<DocumentResponse> getDocumentsByLiftId(Long liftId, Pageable pageable);

    Page<DocumentResponse> getDocumentsByMaintenanceId(Long maintenanceId, Pageable pageable);

    DocumentResponse getDocumentById(Long id);

    DocumentResponse createDocument(DocumentRequest request);

    DocumentResponse updateDocument(Long id, DocumentRequest request);

    void deleteDocument(Long id);
}
