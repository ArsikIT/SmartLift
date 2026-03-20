package com.smartlift.controller;

import com.smartlift.dto.request.MaintenanceRequest;
import com.smartlift.dto.response.MaintenanceResponse;
import com.smartlift.service.MaintenanceService;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<Page<MaintenanceResponse>> getMaintenances(
            @RequestParam(required = false) Long liftId,
            @PageableDefault(size = 20, sort = "requestedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        if (liftId != null) {
            return ResponseEntity.ok(maintenanceService.getMaintenancesByLiftId(liftId, pageable));
        }
        return ResponseEntity.ok(maintenanceService.getAllMaintenances(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MaintenanceResponse> getMaintenanceById(@PathVariable Long id) {
        return ResponseEntity.ok(maintenanceService.getMaintenanceById(id));
    }

    @PostMapping
    public ResponseEntity<MaintenanceResponse> createMaintenance(@Valid @RequestBody MaintenanceRequest request) {
        MaintenanceResponse created = maintenanceService.createMaintenance(request);
        return ResponseEntity.created(URI.create("/api/maintenances/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MaintenanceResponse> updateMaintenance(
            @PathVariable Long id,
            @Valid @RequestBody MaintenanceRequest request
    ) {
        return ResponseEntity.ok(maintenanceService.updateMaintenance(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMaintenance(@PathVariable Long id) {
        maintenanceService.deleteMaintenance(id);
        return ResponseEntity.noContent().build();
    }
}
