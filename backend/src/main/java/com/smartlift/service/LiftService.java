package com.smartlift.service;

import com.smartlift.dto.request.LiftRequest;
import com.smartlift.dto.response.LiftResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LiftService {

    Page<LiftResponse> getAllLifts(String currentUsername, Pageable pageable);

    LiftResponse getLiftById(String currentUsername, Long id);

    LiftResponse getLiftBySerialNumber(String currentUsername, String serialNumber);

    LiftResponse createLift(String currentUsername, LiftRequest request);

    LiftResponse updateLift(String currentUsername, Long id, LiftRequest request);

    void deleteLift(String currentUsername, Long id);
}
