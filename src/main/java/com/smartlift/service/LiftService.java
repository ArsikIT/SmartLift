package com.smartlift.service;

import com.smartlift.dto.LiftRequest;
import com.smartlift.dto.LiftResponse;
import java.util.List;

public interface LiftService {

    List<LiftResponse> getAllLifts();

    LiftResponse getLiftById(Long id);

    LiftResponse createLift(LiftRequest request);

    LiftResponse updateLift(Long id, LiftRequest request);

    void deleteLift(Long id);
}
