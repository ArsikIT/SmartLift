package com.smartlift.service;

import com.smartlift.dto.request.LiftRequest;
import com.smartlift.dto.response.LiftResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LiftService {

    Page<LiftResponse> getAllLifts(Pageable pageable);

    LiftResponse getLiftById(Long id);

    LiftResponse createLift(LiftRequest request);

    LiftResponse updateLift(Long id, LiftRequest request);

    void deleteLift(Long id);
}
