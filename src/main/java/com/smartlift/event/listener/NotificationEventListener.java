package com.smartlift.event.listener;

import com.smartlift.event.MaintenanceCreatedEvent;
import com.smartlift.model.Lift;
import com.smartlift.model.Maintenance;
import com.smartlift.model.Notification;
import com.smartlift.model.User;
import com.smartlift.model.enums.RoleName;
import com.smartlift.repository.NotificationRepository;
import com.smartlift.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;

    @Value("${smartlift.mail.from}")
    private String mailFrom;

    @Async
    @EventListener
    public void handleMaintenanceCreated(MaintenanceCreatedEvent event) {
        Maintenance maintenance = event.getMaintenance();
        Lift lift = maintenance.getLift();

        if (lift.getServiceOrganization() == null) {
            log.warn("Lift {} has no service organization assigned, skipping notifications", lift.getSerialNumber());
            return;
        }

        Long serviceOrgId = lift.getServiceOrganization().getId();
        List<User> technicians = userRepository.findByOrganizationIdAndRole(serviceOrgId, RoleName.SERVICE);

        if (technicians.isEmpty()) {
            log.warn("No SERVICE users found in organization {} for lift {}", serviceOrgId, lift.getSerialNumber());
            return;
        }

        String title = "Lift breakdown: " + lift.getSerialNumber();
        String message = String.format(
                "Maintenance request created for lift %s (model: %s).\nTitle: %s\nDescription: %s",
                lift.getSerialNumber(),
                lift.getModel(),
                maintenance.getTitle(),
                maintenance.getDescription() != null ? maintenance.getDescription() : "—"
        );

        for (User technician : technicians) {
            createNotification(technician, lift, maintenance, title, message);
            sendEmail(technician, title, message);
        }

        log.info("Sent {} notifications for maintenance on lift {}", technicians.size(), lift.getSerialNumber());
    }

    private void createNotification(User recipient, Lift lift, Maintenance maintenance, String title, String message) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setLift(lift);
        notification.setMaintenance(maintenance);
        notification.setTitle(title);
        notification.setMessage(message);
        notificationRepository.save(notification);
    }

    private void sendEmail(User recipient, String title, String message) {
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(mailFrom);
            mail.setTo(recipient.getEmail());
            mail.setSubject("[SmartLift] " + title);
            mail.setText(message);
            mailSender.send(mail);
            log.debug("Email sent to {}", recipient.getEmail());
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", recipient.getEmail(), e.getMessage());
        }
    }
}
