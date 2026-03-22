package com.smartlift.controller;

import com.smartlift.dto.request.OrganizationRequest;
import com.smartlift.dto.response.OrganizationResponse;
import com.smartlift.service.OrganizationService;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class OrganizationController {
    private final OrganizationService organizationService;

    @GetMapping("/me")
    public ResponseEntity<OrganizationResponse> getMyOrganization(Principal principal) {
        return ResponseEntity.ok(organizationService.getMyOrganization(principal.getName()));
    }

    @PutMapping("/me")
    public ResponseEntity<OrganizationResponse> updateMyOrganization(
            Principal principal, @Valid @RequestBody OrganizationRequest request) {
        return ResponseEntity.ok(organizationService.updateMyOrganization(principal.getName(), request));
    }
}
