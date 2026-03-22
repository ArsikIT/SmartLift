package com.smartlift.service.impl;

import com.smartlift.dto.request.LiftEventRequest;
import com.smartlift.dto.response.LiftEventResponse;
import com.smartlift.exception.BadRequestException;
import com.smartlift.exception.ResourceNotFoundException;
import com.smartlift.mapper.SmartLiftMapper;
import com.smartlift.model.Lift;
import com.smartlift.model.LiftEvent;
import com.smartlift.model.User;
import com.smartlift.model.enums.LiftEventType;
import com.smartlift.model.enums.LiftStatus;
import com.smartlift.repository.LiftEventRepository;
import com.smartlift.repository.LiftRepository;
import com.smartlift.repository.UserRepository;
import com.smartlift.service.LiftEventService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class LiftEventServiceImpl implements LiftEventService {

    private final LiftEventRepository liftEventRepository;
    private final LiftRepository liftRepository;
    private final UserRepository userRepository;
    private final SecurityContextHelper securityHelper;

    @Override
    @Transactional(readOnly = true)
    public Page<LiftEventResponse> getAllEvents(String currentUsername, Pageable pageable) {
        User user = securityHelper.resolveUser(currentUsername);
        return liftEventRepository.findAllByOrganizationId(user.getOrganization().getId(), pageable)
                .map(SmartLiftMapper::toLiftEventResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LiftEventResponse> getEventsByLiftId(String currentUsername, Long liftId, Pageable pageable) {
        User user = securityHelper.resolveUser(currentUsername);
        Lift lift = getLiftOrThrow(liftId);
        securityHelper.checkLiftBelongsToOrg(lift, user.getOrganization().getId());
        return liftEventRepository.findAllByLiftId(liftId, pageable)
                .map(SmartLiftMapper::toLiftEventResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public LiftEventResponse getEventById(String currentUsername, Long id) {
        User user = securityHelper.resolveUser(currentUsername);
        LiftEvent event = getDetailedEventOrThrow(id);
        securityHelper.checkLiftBelongsToOrg(event.getLift(), user.getOrganization().getId());
        return SmartLiftMapper.toLiftEventResponse(event);
    }

    @Override
    public LiftEventResponse createEvent(String currentUsername, LiftEventRequest request) {
        User user = securityHelper.resolveUser(currentUsername);
        Lift lift = getLiftOrThrow(request.getLiftId());
        securityHelper.checkLiftBelongsToOrg(lift, user.getOrganization().getId());

        LiftEvent event = new LiftEvent();
        applyEventRequest(event, request, lift);

        LiftEvent savedEvent = liftEventRepository.saveAndFlush(event);
        refreshLiftStatus(lift.getId());
        return SmartLiftMapper.toLiftEventResponse(getDetailedEventOrThrow(savedEvent.getId()));
    }

    @Override
    public LiftEventResponse updateEvent(String currentUsername, Long id, LiftEventRequest request) {
        User user = securityHelper.resolveUser(currentUsername);
        LiftEvent existingEvent = getEventOrThrow(id);
        securityHelper.checkLiftBelongsToOrg(existingEvent.getLift(), user.getOrganization().getId());

        Long previousLiftId = existingEvent.getLift().getId();
        Lift lift = getLiftOrThrow(request.getLiftId());
        securityHelper.checkLiftBelongsToOrg(lift, user.getOrganization().getId());

        applyEventRequest(existingEvent, request, lift);

        LiftEvent savedEvent = liftEventRepository.saveAndFlush(existingEvent);
        refreshLiftStatus(previousLiftId);
        if (!previousLiftId.equals(lift.getId())) {
            refreshLiftStatus(lift.getId());
        }
        return SmartLiftMapper.toLiftEventResponse(getDetailedEventOrThrow(savedEvent.getId()));
    }

    @Override
    public void deleteEvent(String currentUsername, Long id) {
        User user = securityHelper.resolveUser(currentUsername);
        LiftEvent existingEvent = getEventOrThrow(id);
        securityHelper.checkLiftBelongsToOrg(existingEvent.getLift(), user.getOrganization().getId());

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
