package com.smartlift.dto;

import com.smartlift.model.LiftStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LiftRequest {

    @NotBlank
    @Size(max = 100)
    private String serialNumber;

    @NotBlank
    @Size(max = 100)
    private String model;

    @Size(max = 100)
    private String manufacturer;

    private LiftStatus status;
    private Long manufacturerOrganizationId;
    private Long serviceOrganizationId;
    private Long managementOrganizationId;
}
