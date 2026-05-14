package com.smartlift.event.listener;

import com.smartlift.event.MaintenanceCreatedEvent;
import com.smartlift.model.Lift;
import com.smartlift.model.Maintenance;
import com.smartlift.model.Notification;
import com.smartlift.model.Organization;
import com.smartlift.model.User;
import com.smartlift.model.enums.RoleName;
import com.smartlift.repository.NotificationRepository;
import com.smartlift.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;
    @Mock
    private JavaMailSender mailSender;

    @Test
    void handleMaintenanceCreated_createsInAppNotificationsWhenEmailIsDisabled() {
        NotificationEventListener listener =
                new NotificationEventListener(notificationRepository, userRepository, mailSenderProvider);
        ReflectionTestUtils.setField(listener, "mailEnabled", false);
        ReflectionTestUtils.setField(listener, "mailFrom", "no-reply@smartlift.local");

        Maintenance maintenance = new Maintenance();
        maintenance.setTitle("Emergency repair");
        maintenance.setDescription("Cabin stuck between floors");

        Organization serviceOrg = new Organization();
        serviceOrg.setId(10L);

        Lift lift = new Lift();
        lift.setSerialNumber("SMOKE-SERIAL");
        lift.setModel("Model X");
        lift.setServiceOrganization(serviceOrg);
        maintenance.setLift(lift);

        User technician1 = new User();
        technician1.setId(1L);
        technician1.setEmail("tech1@test.com");
        User technician2 = new User();
        technician2.setId(2L);
        technician2.setEmail("tech2@test.com");

        when(userRepository.findByOrganizationIdAndRole(10L, RoleName.SERVICE))
                .thenReturn(List.of(technician1, technician2));

        listener.handleMaintenanceCreated(new MaintenanceCreatedEvent(this, maintenance));

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(2)).save(notificationCaptor.capture());
        verify(mailSenderProvider, never()).getIfAvailable();
        verify(mailSender, never()).send(any(org.springframework.mail.SimpleMailMessage.class));

        List<Notification> savedNotifications = notificationCaptor.getAllValues();
        assertThat(savedNotifications).hasSize(2);
        assertThat(savedNotifications)
                .extracting(notification -> notification.getRecipient().getId())
                .containsExactly(1L, 2L);
        assertThat(savedNotifications)
                .extracting(Notification::getTitle)
                .containsOnly("Lift breakdown: SMOKE-SERIAL");
    }
}
