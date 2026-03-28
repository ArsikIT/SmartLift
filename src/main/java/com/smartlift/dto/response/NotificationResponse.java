package com.smartlift.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationResponse {
    private Long id;
    private String title;
    private String message;
    private boolean isRead;
    private Long liftId;
    private Long maintenanceId;
    private LocalDateTime createdAt;
}
