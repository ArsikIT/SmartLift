package com.smartlift.dto;

import com.smartlift.model.MaintenanceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MaintenanceRequest {

    @NotNull
    private Long liftId;

    @NotBlank
    @Size(max = 255)
    private String title;

    @Size(max = 1000)
    private String description;

    private MaintenanceStatus status;
    private Long assignedTechnicianId;
    private Long requestedByUserId;
}
