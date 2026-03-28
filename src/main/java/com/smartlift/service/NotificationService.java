package com.smartlift.service;

import com.smartlift.dto.response.NotificationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    Page<NotificationResponse> getMyNotifications(String currentUsername, Pageable pageable);

    long getUnreadCount(String currentUsername);

    NotificationResponse markAsRead(String currentUsername, Long notificationId);

    void markAllAsRead(String currentUsername);
}
