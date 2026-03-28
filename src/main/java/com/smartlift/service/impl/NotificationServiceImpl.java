package com.smartlift.service.impl;

import com.smartlift.dto.response.NotificationResponse;
import com.smartlift.exception.ResourceNotFoundException;
import com.smartlift.mapper.SmartLiftMapper;
import com.smartlift.model.Notification;
import com.smartlift.model.User;
import com.smartlift.repository.NotificationRepository;
import com.smartlift.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final SecurityContextHelper securityHelper;

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(String currentUsername, Pageable pageable) {
        User user = securityHelper.resolveUser(currentUsername);
        return notificationRepository.findAllByRecipientIdOrderByCreatedAtDesc(user.getId(), pageable)
                .map(SmartLiftMapper::toNotificationResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(String currentUsername) {
        User user = securityHelper.resolveUser(currentUsername);
        return notificationRepository.countByRecipientIdAndIsReadFalse(user.getId());
    }

    @Override
    public NotificationResponse markAsRead(String currentUsername, Long notificationId) {
        User user = securityHelper.resolveUser(currentUsername);
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));

        if (!notification.getRecipient().getId().equals(user.getId())) {
            throw new AccessDeniedException("You can only mark your own notifications as read");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
        return SmartLiftMapper.toNotificationResponse(notification);
    }

    @Override
    public void markAllAsRead(String currentUsername) {
        User user = securityHelper.resolveUser(currentUsername);
        notificationRepository.markAllAsReadByRecipientId(user.getId());
    }
}
