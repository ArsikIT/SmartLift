package com.smartlift.service;

import com.smartlift.dto.LiftEventRequest;
import com.smartlift.dto.LiftEventResponse;
import java.util.List;

public interface LiftEventService {

    List<LiftEventResponse> getAllEvents();

    List<LiftEventResponse> getEventsByLiftId(Long liftId);

    LiftEventResponse getEventById(Long id);

    LiftEventResponse createEvent(LiftEventRequest request);

    LiftEventResponse updateEvent(Long id, LiftEventRequest request);

    void deleteEvent(Long id);
}
