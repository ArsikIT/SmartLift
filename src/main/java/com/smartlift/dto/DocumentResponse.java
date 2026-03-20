package com.smartlift.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DocumentResponse {

    private final Long id;
    private final String fileName;
    private final String filePath;
    private final String contentType;
    private final Long liftId;
    private final Long maintenanceId;
    private final UserSummaryResponse uploadedBy;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
