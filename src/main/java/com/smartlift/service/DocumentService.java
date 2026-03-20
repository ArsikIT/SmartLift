package com.smartlift.service;

import com.smartlift.dto.DocumentRequest;
import com.smartlift.dto.DocumentResponse;
import java.util.List;

public interface DocumentService {

    List<DocumentResponse> getAllDocuments();

    List<DocumentResponse> getDocumentsByLiftId(Long liftId);

    List<DocumentResponse> getDocumentsByMaintenanceId(Long maintenanceId);

    DocumentResponse getDocumentById(Long id);

    DocumentResponse createDocument(DocumentRequest request);

    DocumentResponse updateDocument(Long id, DocumentRequest request);

    void deleteDocument(Long id);
}
