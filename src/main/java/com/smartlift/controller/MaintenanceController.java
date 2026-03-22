package com.smartlift.controller;

import com.smartlift.dto.request.MaintenanceRequest;
import com.smartlift.dto.response.MaintenanceResponse;
import com.smartlift.service.MaintenanceService;
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
@RequestMapping("/api/maintenances")
@RequiredArgsConstructor
public class MaintenanceController {
    private final MaintenanceService maintenanceService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SERVICE', 'MANAGEMENT')")
    public ResponseEntity<Page<MaintenanceResponse>> getMaintenances(
            Principal principal,
            @RequestParam(required = false) Long liftId,
            @PageableDefault(size = 20, sort = "requestedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        if (liftId != null) {
            return ResponseEntity.ok(maintenanceService.getMaintenancesByLiftId(principal.getName(), liftId, pageable));
        }
        return ResponseEntity.ok(maintenanceService.getAllMaintenances(principal.getName(), pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SERVICE', 'MANAGEMENT')")
    public ResponseEntity<MaintenanceResponse> getMaintenanceById(Principal principal, @PathVariable Long id) {
        return ResponseEntity.ok(maintenanceService.getMaintenanceById(principal.getName(), id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SERVICE', 'MANAGEMENT')")
    public ResponseEntity<MaintenanceResponse> createMaintenance(Principal principal, @Valid @RequestBody MaintenanceRequest request) {
        MaintenanceResponse created = maintenanceService.createMaintenance(principal.getName(), request);
        return ResponseEntity.created(URI.create("/api/maintenances/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SERVICE')")
    public ResponseEntity<MaintenanceResponse> updateMaintenance(Principal principal, @PathVariable Long id, @Valid @RequestBody MaintenanceRequest request) {
        return ResponseEntity.ok(maintenanceService.updateMaintenance(principal.getName(), id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SERVICE')")
    public ResponseEntity<Void> deleteMaintenance(Principal principal, @PathVariable Long id) {
        maintenanceService.deleteMaintenance(principal.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
