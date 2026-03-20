package com.smartlift.dto.response;

import com.smartlift.model.enums.LiftEventType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LiftEventResponse {

    private final Long id;
    private final Long liftId;
    private final String liftSerialNumber;
    private final LiftEventType type;
    private final LocalDateTime eventAt;
    private final String description;
    private final UserSummaryResponse performedBy;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
