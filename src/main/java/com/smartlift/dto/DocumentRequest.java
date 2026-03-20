package com.smartlift.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DocumentRequest {

    @NotBlank
    @Size(max = 255)
    private String fileName;

    @NotBlank
    @Size(max = 500)
    private String filePath;

    @Size(max = 100)
    private String contentType;

    private Long liftId;
    private Long maintenanceId;
    private Long uploadedByUserId;
}
