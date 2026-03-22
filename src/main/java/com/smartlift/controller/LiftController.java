package com.smartlift.controller;

import com.smartlift.dto.request.LiftRequest;
import com.smartlift.dto.response.LiftResponse;
import jakarta.validation.Valid;
import com.smartlift.service.LiftService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lifts")
@RequiredArgsConstructor
public class LiftController {
    private final LiftService liftService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANUFACTURER', 'SERVICE', 'MANAGEMENT')")
    public ResponseEntity<Page<LiftResponse>> getAllLifts(
            Principal principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(liftService.getAllLifts(principal.getName(), pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANUFACTURER', 'SERVICE', 'MANAGEMENT')")
    public ResponseEntity<LiftResponse> getLiftById(Principal principal, @PathVariable Long id) {
        return ResponseEntity.ok(liftService.getLiftById(principal.getName(), id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANUFACTURER')")
    public ResponseEntity<LiftResponse> createLift(Principal principal, @Valid @RequestBody LiftRequest request) {
        LiftResponse createdLift = liftService.createLift(principal.getName(), request);
        return ResponseEntity.created(URI.create("/api/lifts/" + createdLift.getId())).body(createdLift);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANUFACTURER')")
    public ResponseEntity<LiftResponse> updateLift(Principal principal, @PathVariable Long id, @Valid @RequestBody LiftRequest request) {
        return ResponseEntity.ok(liftService.updateLift(principal.getName(), id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANUFACTURER')")
    public ResponseEntity<Void> deleteLift(Principal principal, @PathVariable Long id) {
        liftService.deleteLift(principal.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
