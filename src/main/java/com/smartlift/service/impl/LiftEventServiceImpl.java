package com.smartlift.service.impl;

import com.smartlift.dto.LiftEventRequest;
import com.smartlift.dto.LiftEventResponse;
import com.smartlift.exception.BadRequestException;
import com.smartlift.exception.ResourceNotFoundException;
import com.smartlift.mapper.SmartLiftMapper;
import com.smartlift.model.Lift;
import com.smartlift.model.LiftEvent;
import com.smartlift.model.LiftEventType;
import com.smartlift.model.LiftStatus;
import com.smartlift.model.User;
import com.smartlift.repository.LiftEventRepository;
import com.smartlift.repository.LiftRepository;
import com.smartlift.repository.UserRepository;
import com.smartlift.service.LiftEventService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class LiftEventServiceImpl implements LiftEventService {

    private final LiftEventRepository liftEventRepository;
    private final LiftRepository liftRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<LiftEventResponse> getAllEvents() {
        return liftEventRepository.findAllByOrderByEventAtDesc().stream()
                .map(SmartLiftMapper::toLiftEventResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LiftEventResponse> getEventsByLiftId(Long liftId) {
        ensureLiftExists(liftId);
        return liftEventRepository.findAllByLiftIdOrderByEventAtDesc(liftId).stream()
                .map(SmartLiftMapper::toLiftEventResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LiftEventResponse getEventById(Long id) {
        return SmartLiftMapper.toLiftEventResponse(getDetailedEventOrThrow(id));
    }

    @Override
    public LiftEventResponse createEvent(LiftEventRequest request) {
        Lift lift = getLiftOrThrow(request.getLiftId());
        LiftEvent event = new LiftEvent();
        applyEventRequest(event, request, lift);

        LiftEvent savedEvent = liftEventRepository.saveAndFlush(event);
        refreshLiftStatus(lift.getId());
        return SmartLiftMapper.toLiftEventResponse(getDetailedEventOrThrow(savedEvent.getId()));
    }

    @Override
    public LiftEventResponse updateEvent(Long id, LiftEventRequest request) {
        LiftEvent existingEvent = getEventOrThrow(id);
        Long previousLiftId = existingEvent.getLift().getId();
        Lift lift = getLiftOrThrow(request.getLiftId());
        applyEventRequest(existingEvent, request, lift);

        LiftEvent savedEvent = liftEventRepository.saveAndFlush(existingEvent);
        refreshLiftStatus(previousLiftId);
        if (!previousLiftId.equals(lift.getId())) {
            refreshLiftStatus(lift.getId());
        }
        return SmartLiftMapper.toLiftEventResponse(getDetailedEventOrThrow(savedEvent.getId()));
    }

    @Override
    public void deleteEvent(Long id) {
        LiftEvent existingEvent = getEventOrThrow(id);
        Long liftId = existingEvent.getLift().getId();
        liftEventRepository.delete(existingEvent);
        liftEventRepository.flush();
        refreshLiftStatus(liftId);
    }

    private void applyEventRequest(LiftEvent event, LiftEventRequest request, Lift lift) {
        validateEventBusinessRules(request);
        event.setLift(lift);
        event.setType(request.getType());
        event.setEventAt(request.getEventAt() != null ? request.getEventAt() : LocalDateTime.now());
        event.setDescription(request.getDescription().trim());
        event.setPerformedBy(resolveUser(request.getPerformedByUserId()));
    }

    private void validateEventBusinessRules(LiftEventRequest request) {
        LocalDateTime eventAt = request.getEventAt();
        if (eventAt != null && eventAt.isAfter(LocalDateTime.now())) {
            throw new BadRequestException("Event time cannot be in the future");
        }
    }

    private void refreshLiftStatus(Long liftId) {
        Lift lift = getLiftOrThrow(liftId);
        LiftStatus recalculatedStatus = liftEventRepository.findTopByLiftIdOrderByEventAtDescCreatedAtDesc(liftId)
                .map(LiftEvent::getType)
                .map(this::mapEventTypeToLiftStatus)
                .orElse(LiftStatus.CREATED);
        lift.setStatus(recalculatedStatus);
        liftRepository.save(lift);
    }

    private User resolveUser(Long userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    private LiftStatus mapEventTypeToLiftStatus(LiftEventType eventType) {
        if (eventType == LiftEventType.CREATED) {
            return LiftStatus.CREATED;
        }
        if (eventType == LiftEventType.INSTALLED) {
            return LiftStatus.INSTALLED;
        }
        if (eventType == LiftEventType.FAULT) {
            return LiftStatus.FAULTY;
        }
        if (eventType == LiftEventType.REPAIR) {
            return LiftStatus.IN_REPAIR;
        }
        return LiftStatus.CREATED;
    }

    private void ensureLiftExists(Long liftId) {
        if (!liftRepository.existsById(liftId)) {
            throw new ResourceNotFoundException("Lift not found: " + liftId);
        }
    }

    private Lift getLiftOrThrow(Long id) {
        return liftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lift not found: " + id));
    }

    private LiftEvent getEventOrThrow(Long id) {
        return liftEventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lift event not found: " + id));
    }

    private LiftEvent getDetailedEventOrThrow(Long id) {
        return liftEventRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lift event not found: " + id));
    }
}
