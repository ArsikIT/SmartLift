package com.smartlift.controller;

import com.smartlift.dto.request.DocumentRequest;
import com.smartlift.dto.response.DocumentResponse;
import com.smartlift.service.DocumentService;
import jakarta.validation.Valid;
import java.net.URI;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {
    private final DocumentService documentService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SERVICE', 'MANAGEMENT')")
    public ResponseEntity<Page<DocumentResponse>> getDocuments(
            Principal principal,
            @RequestParam(required = false) Long liftId,
            @RequestParam(required = false) Long maintenanceId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        if (liftId != null) {
            return ResponseEntity.ok(documentService.getDocumentsByLiftId(principal.getName(), liftId, pageable));
        }
        if (maintenanceId != null) {
            return ResponseEntity.ok(documentService.getDocumentsByMaintenanceId(principal.getName(), maintenanceId, pageable));
        }
        return ResponseEntity.ok(documentService.getAllDocuments(principal.getName(), pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SERVICE', 'MANAGEMENT')")
    public ResponseEntity<DocumentResponse> getDocumentById(Principal principal, @PathVariable Long id) {
        return ResponseEntity.ok(documentService.getDocumentById(principal.getName(), id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SERVICE', 'MANAGEMENT')")
    public ResponseEntity<DocumentResponse> createDocument(Principal principal, @Valid @RequestBody DocumentRequest request) {
        DocumentResponse created = documentService.createDocument(principal.getName(), request);
        return ResponseEntity.created(URI.create("/api/documents/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SERVICE')")
    public ResponseEntity<DocumentResponse> updateDocument(Principal principal, @PathVariable Long id, @Valid @RequestBody DocumentRequest request) {
        return ResponseEntity.ok(documentService.updateDocument(principal.getName(), id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SERVICE')")
    public ResponseEntity<Void> deleteDocument(Principal principal, @PathVariable Long id) {
        documentService.deleteDocument(principal.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
