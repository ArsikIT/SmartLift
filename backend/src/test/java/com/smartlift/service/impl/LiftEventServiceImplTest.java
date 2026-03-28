package com.smartlift.service.impl;

import com.smartlift.dto.request.LiftEventRequest;
import com.smartlift.dto.response.LiftEventResponse;
import com.smartlift.exception.BadRequestException;
import com.smartlift.exception.ResourceNotFoundException;
import com.smartlift.model.Lift;
import com.smartlift.model.LiftEvent;
import com.smartlift.model.Organization;
import com.smartlift.model.User;
import com.smartlift.model.enums.LiftEventType;
import com.smartlift.model.enums.LiftStatus;
import com.smartlift.repository.LiftEventRepository;
import com.smartlift.repository.LiftRepository;
import com.smartlift.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LiftEventServiceImplTest {

    @Mock
    private LiftEventRepository liftEventRepository;
    @Mock
    private LiftRepository liftRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SecurityContextHelper securityHelper;

    @InjectMocks
    private LiftEventServiceImpl liftEventService;

    private final Pageable pageable = PageRequest.of(0, 20);

    @Test
    void getAllEvents_returnsPageOfEvents() {
        User user = createUserWithOrg(1L, "admin", 10L);
        when(securityHelper.resolveUser("admin")).thenReturn(user);

        Lift lift = createLift(1L, "SN-001");
        LiftEvent event = createEvent(1L, lift, LiftEventType.CREATED);
        Page<LiftEvent> page = new PageImpl<>(List.of(event));
        when(liftEventRepository.findAllByOrganizationId(10L, pageable)).thenReturn(page);

        Page<LiftEventResponse> result = liftEventService.getAllEvents("admin", pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getEventsByLiftId_returnsFilteredEvents() {
        User user = createUserWithOrg(1L, "admin", 10L);
        when(securityHelper.resolveUser("admin")).thenReturn(user);

        Lift lift = createLift(5L, "SN-005");
        when(liftRepository.findById(5L)).thenReturn(Optional.of(lift));

        LiftEvent event = createEvent(1L, lift, LiftEventType.FAULT);
        Page<LiftEvent> page = new PageImpl<>(List.of(event));
        when(liftEventRepository.findAllByLiftId(5L, pageable)).thenReturn(page);

        Page<LiftEventResponse> result = liftEventService.getEventsByLiftId("admin", 5L, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getType()).isEqualTo(LiftEventType.FAULT);
    }

    @Test
    void getEventById_returnsEvent() {
        User user = createUserWithOrg(1L, "admin", 10L);
        when(securityHelper.resolveUser("admin")).thenReturn(user);

        Lift lift = createLift(5L, "SN-005");
        LiftEvent event = createEvent(20L, lift, LiftEventType.REPAIR);
        when(liftEventRepository.findDetailedById(20L)).thenReturn(Optional.of(event));

        LiftEventResponse result = liftEventService.getEventById("admin", 20L);

        assertThat(result.getId()).isEqualTo(20L);
        assertThat(result.getType()).isEqualTo(LiftEventType.REPAIR);
    }

    @Test
    void getEventById_throwsWhenNotFound() {
        User user = createUserWithOrg(1L, "admin", 10L);
        when(securityHelper.resolveUser("admin")).thenReturn(user);
        when(liftEventRepository.findDetailedById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> liftEventService.getEventById("admin", 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createEvent_createsEventAndRefreshesLiftStatus() {
        User user = createUserWithOrg(1L, "admin", 10L);
        when(securityHelper.resolveUser("admin")).thenReturn(user);

        Lift lift = createLift(5L, "SN-005");
        lift.setStatus(LiftStatus.ACTIVE);
        when(liftRepository.findById(5L)).thenReturn(Optional.of(lift));

        LiftEventRequest request = new LiftEventRequest();
        request.setLiftId(5L);
        request.setType(LiftEventType.FAULT);
        request.setDescription("Motor failure");
        request.setEventAt(LocalDateTime.now().minusHours(1));

        LiftEvent savedEvent = createEvent(30L, lift, LiftEventType.FAULT);
        when(liftEventRepository.saveAndFlush(any(LiftEvent.class))).thenReturn(savedEvent);
        when(liftEventRepository.findDetailedById(30L)).thenReturn(Optional.of(savedEvent));

        // Refresh lift status
        LiftEvent latestEvent = createEvent(30L, lift, LiftEventType.FAULT);
        when(liftEventRepository.findTopByLiftIdOrderByEventAtDescCreatedAtDesc(5L))
                .thenReturn(Optional.of(latestEvent));

        LiftEventResponse result = liftEventService.createEvent("admin", request);

        assertThat(result.getId()).isEqualTo(30L);
        verify(liftRepository).save(any(Lift.class));
    }

    @Test
    void createEvent_throwsWhenEventTimeInFuture() {
        User user = createUserWithOrg(1L, "admin", 10L);
        when(securityHelper.resolveUser("admin")).thenReturn(user);

        Lift lift = createLift(5L, "SN-005");
        when(liftRepository.findById(5L)).thenReturn(Optional.of(lift));

        LiftEventRequest request = new LiftEventRequest();
        request.setLiftId(5L);
        request.setType(LiftEventType.CREATED);
        request.setDescription("Future event");
        request.setEventAt(LocalDateTime.now().plusDays(1));

        assertThatThrownBy(() -> liftEventService.createEvent("admin", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Event time cannot be in the future");
    }

    @Test
    void createEvent_resolvesPerformedByUser() {
        User user = createUserWithOrg(1L, "admin", 10L);
        when(securityHelper.resolveUser("admin")).thenReturn(user);

        Lift lift = createLift(5L, "SN-005");
        lift.setStatus(LiftStatus.FAULTY);
        when(liftRepository.findById(5L)).thenReturn(Optional.of(lift));

        User performer = new User();
        performer.setId(7L);
        performer.setUsername("tech");
        when(userRepository.findById(7L)).thenReturn(Optional.of(performer));

        LiftEventRequest request = new LiftEventRequest();
        request.setLiftId(5L);
        request.setType(LiftEventType.REPAIR);
        request.setDescription("Repaired");
        request.setPerformedByUserId(7L);

        LiftEvent savedEvent = createEvent(31L, lift, LiftEventType.REPAIR);
        savedEvent.setPerformedBy(performer);
        when(liftEventRepository.saveAndFlush(any(LiftEvent.class))).thenReturn(savedEvent);
        when(liftEventRepository.findDetailedById(31L)).thenReturn(Optional.of(savedEvent));
        when(liftEventRepository.findTopByLiftIdOrderByEventAtDescCreatedAtDesc(5L))
                .thenReturn(Optional.of(savedEvent));

        LiftEventResponse result = liftEventService.createEvent("admin", request);

        assertThat(result.getPerformedBy()).isNotNull();
    }

    @Test
    void createEvent_throwsWhenPerformedByUserNotFound() {
        User user = createUserWithOrg(1L, "admin", 10L);
        when(securityHelper.resolveUser("admin")).thenReturn(user);

        Lift lift = createLift(5L, "SN-005");
        lift.setStatus(LiftStatus.ACTIVE);
        when(liftRepository.findById(5L)).thenReturn(Optional.of(lift));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        LiftEventRequest request = new LiftEventRequest();
        request.setLiftId(5L);
        request.setType(LiftEventType.FAULT);
        request.setDescription("Fault");
        request.setPerformedByUserId(999L);

        assertThatThrownBy(() -> liftEventService.createEvent("admin", request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void deleteEvent_deletesAndRefreshesStatus() {
        User user = createUserWithOrg(1L, "admin", 10L);
        when(securityHelper.resolveUser("admin")).thenReturn(user);

        Lift lift = createLift(5L, "SN-005");
        lift.setStatus(LiftStatus.FAULTY);
        LiftEvent event = createEvent(30L, lift, LiftEventType.FAULT);
        when(liftEventRepository.findById(30L)).thenReturn(Optional.of(event));
        when(liftRepository.findById(5L)).thenReturn(Optional.of(lift));
        when(liftEventRepository.findTopByLiftIdOrderByEventAtDescCreatedAtDesc(5L))
                .thenReturn(Optional.empty());

        liftEventService.deleteEvent("admin", 30L);

        verify(liftEventRepository).delete(event);
        verify(liftRepository).save(any(Lift.class));
    }

    @Test
    void updateEvent_refreshesBothLiftsWhenLiftChanges() {
        User user = createUserWithOrg(1L, "admin", 10L);
        when(securityHelper.resolveUser("admin")).thenReturn(user);

        Lift oldLift = createLift(5L, "SN-OLD");
        oldLift.setStatus(LiftStatus.FAULTY);
        Lift newLift = createLift(6L, "SN-NEW");

        LiftEvent existingEvent = createEvent(30L, oldLift, LiftEventType.FAULT);
        when(liftEventRepository.findById(30L)).thenReturn(Optional.of(existingEvent));
        when(liftRepository.findById(6L)).thenReturn(Optional.of(newLift));
        when(liftRepository.findById(5L)).thenReturn(Optional.of(oldLift));

        LiftEventRequest request = new LiftEventRequest();
        request.setLiftId(6L);
        request.setType(LiftEventType.INSTALLED);
        request.setDescription("Installed at new site");

        LiftEvent savedEvent = createEvent(30L, newLift, LiftEventType.INSTALLED);
        when(liftEventRepository.saveAndFlush(any(LiftEvent.class))).thenReturn(savedEvent);
        when(liftEventRepository.findDetailedById(30L)).thenReturn(Optional.of(savedEvent));
        when(liftEventRepository.findTopByLiftIdOrderByEventAtDescCreatedAtDesc(5L))
                .thenReturn(Optional.empty());
        when(liftEventRepository.findTopByLiftIdOrderByEventAtDescCreatedAtDesc(6L))
                .thenReturn(Optional.of(savedEvent));

        liftEventService.updateEvent("admin", 30L, request);

        // Should refresh both old and new lift statuses
        verify(liftRepository, times(2)).save(any(Lift.class));
    }

    private User createUserWithOrg(Long userId, String username, Long orgId) {
        Organization org = new Organization();
        org.setId(orgId);
        User user = new User();
        user.setId(userId);
        user.setUsername(username);
        user.setOrganization(org);
        return user;
    }

    private Lift createLift(Long id, String serialNumber) {
        Lift lift = new Lift();
        lift.setId(id);
        lift.setSerialNumber(serialNumber);
        lift.setStatus(LiftStatus.CREATED);
        return lift;
    }

    private LiftEvent createEvent(Long id, Lift lift, LiftEventType type) {
        LiftEvent event = new LiftEvent();
        event.setId(id);
        event.setLift(lift);
        event.setType(type);
        event.setEventAt(LocalDateTime.now().minusHours(1));
        event.setDescription("Test event");
        return event;
    }
}
