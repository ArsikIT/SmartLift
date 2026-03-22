package com.smartlift.controller;

import com.smartlift.dto.request.LiftEventRequest;
import com.smartlift.dto.response.LiftEventResponse;
import jakarta.validation.Valid;
import com.smartlift.service.LiftEventService;
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
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class LiftEventController {
    private final LiftEventService liftEventService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SERVICE', 'MANUFACTURER')")
    public ResponseEntity<Page<LiftEventResponse>> getEvents(
            Principal principal,
            @RequestParam(required = false) Long liftId,
            @PageableDefault(size = 20, sort = "eventAt", direction = Sort.Direction.DESC) Pageable pageable) {
        if (liftId != null) {
            return ResponseEntity.ok(liftEventService.getEventsByLiftId(principal.getName(), liftId, pageable));
        }
        return ResponseEntity.ok(liftEventService.getAllEvents(principal.getName(), pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SERVICE', 'MANUFACTURER')")
    public ResponseEntity<LiftEventResponse> getEventById(Principal principal, @PathVariable Long id) {
        return ResponseEntity.ok(liftEventService.getEventById(principal.getName(), id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SERVICE')")
    public ResponseEntity<LiftEventResponse> createEvent(Principal principal, @Valid @RequestBody LiftEventRequest request) {
        LiftEventResponse createdEvent = liftEventService.createEvent(principal.getName(), request);
        return ResponseEntity.created(URI.create("/api/events/" + createdEvent.getId())).body(createdEvent);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SERVICE')")
    public ResponseEntity<LiftEventResponse> updateEvent(Principal principal, @PathVariable Long id, @Valid @RequestBody LiftEventRequest request) {
        return ResponseEntity.ok(liftEventService.updateEvent(principal.getName(), id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SERVICE')")
    public ResponseEntity<Void> deleteEvent(Principal principal, @PathVariable Long id) {
        liftEventService.deleteEvent(principal.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
