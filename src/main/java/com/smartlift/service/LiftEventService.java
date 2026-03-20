package com.smartlift.service;

import com.smartlift.dto.request.LiftEventRequest;
import com.smartlift.dto.response.LiftEventResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LiftEventService {

    Page<LiftEventResponse> getAllEvents(Pageable pageable);

    Page<LiftEventResponse> getEventsByLiftId(Long liftId, Pageable pageable);

    LiftEventResponse getEventById(Long id);

    LiftEventResponse createEvent(LiftEventRequest request);

    LiftEventResponse updateEvent(Long id, LiftEventRequest request);

    void deleteEvent(Long id);
}
