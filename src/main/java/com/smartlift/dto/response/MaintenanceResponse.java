package com.smartlift.dto.response;

import com.smartlift.model.enums.MaintenanceStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MaintenanceResponse {

    private final Long id;
    private final Long liftId;
    private final String liftSerialNumber;
    private final String title;
    private final String description;
    private final MaintenanceStatus status;
    private final UserSummaryResponse assignedTechnician;
    private final UserSummaryResponse requestedBy;
    private final LocalDateTime requestedAt;
    private final LocalDateTime startedAt;
    private final LocalDateTime completedAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
