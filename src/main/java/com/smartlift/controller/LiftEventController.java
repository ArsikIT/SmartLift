package com.smartlift.controller;

import com.smartlift.dto.request.LiftEventRequest;
import com.smartlift.dto.response.LiftEventResponse;
import jakarta.validation.Valid;
import com.smartlift.service.LiftEventService;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class LiftEventController {

    private final LiftEventService liftEventService;

    @GetMapping
    public ResponseEntity<List<LiftEventResponse>> getEvents(@RequestParam(required = false) Long liftId) {
        if (liftId != null) {
            return ResponseEntity.ok(liftEventService.getEventsByLiftId(liftId));
        }
        return ResponseEntity.ok(liftEventService.getAllEvents());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LiftEventResponse> getEventById(@PathVariable Long id) {
        return ResponseEntity.ok(liftEventService.getEventById(id));
    }

    @PostMapping
    public ResponseEntity<LiftEventResponse> createEvent(@Valid @RequestBody LiftEventRequest request) {
        LiftEventResponse createdEvent = liftEventService.createEvent(request);
        return ResponseEntity.created(URI.create("/api/events/" + createdEvent.getId())).body(createdEvent);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LiftEventResponse> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody LiftEventRequest request
    ) {
        return ResponseEntity.ok(liftEventService.updateEvent(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        liftEventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }
}
