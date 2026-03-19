package com.smartlift.controller;

import com.smartlift.dto.LiftRequest;
import com.smartlift.dto.LiftResponse;
import jakarta.validation.Valid;
import com.smartlift.service.LiftService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lifts")
@RequiredArgsConstructor
public class LiftController {

    private final LiftService liftService;

    @GetMapping
    public ResponseEntity<List<LiftResponse>> getAllLifts() {
        return ResponseEntity.ok(liftService.getAllLifts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LiftResponse> getLiftById(@PathVariable Long id) {
        return ResponseEntity.ok(liftService.getLiftById(id));
    }

    @PostMapping
    public ResponseEntity<LiftResponse> createLift(@Valid @RequestBody LiftRequest request) {
        LiftResponse createdLift = liftService.createLift(request);
        return ResponseEntity.created(URI.create("/api/lifts/" + createdLift.getId())).body(createdLift);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LiftResponse> updateLift(@PathVariable Long id, @Valid @RequestBody LiftRequest request) {
        return ResponseEntity.ok(liftService.updateLift(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLift(@PathVariable Long id) {
        liftService.deleteLift(id);
        return ResponseEntity.noContent().build();
    }
}
