package com.smartlift.controller;

import com.smartlift.dto.response.NotificationResponse;
import com.smartlift.service.NotificationService;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getMyNotifications(
            Principal principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(notificationService.getMyNotifications(principal.getName(), pageable));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount(Principal principal) {
        return ResponseEntity.ok(notificationService.getUnreadCount(principal.getName()));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(Principal principal, @PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markAsRead(principal.getName(), id));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(Principal principal) {
        notificationService.markAllAsRead(principal.getName());
        return ResponseEntity.noContent().build();
    }
}
