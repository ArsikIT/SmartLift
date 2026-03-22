package com.smartlift.service;

import com.smartlift.dto.request.LiftEventRequest;
import com.smartlift.dto.response.LiftEventResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LiftEventService {

    Page<LiftEventResponse> getAllEvents(String currentUsername, Pageable pageable);

    Page<LiftEventResponse> getEventsByLiftId(String currentUsername, Long liftId, Pageable pageable);

    LiftEventResponse getEventById(String currentUsername, Long id);

    LiftEventResponse createEvent(String currentUsername, LiftEventRequest request);

    LiftEventResponse updateEvent(String currentUsername, Long id, LiftEventRequest request);

    void deleteEvent(String currentUsername, Long id);
}
