package com.smartlift.dto;

import com.smartlift.model.LiftEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LiftEventRequest {

    @NotNull
    private Long liftId;

    @NotNull
    private LiftEventType type;

    private LocalDateTime eventAt;

    @NotBlank
    @Size(max = 500)
    private String description;

    private Long performedByUserId;
}
