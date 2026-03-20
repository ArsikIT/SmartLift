package com.smartlift.dto;

import com.smartlift.model.OrganizationType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OrganizationRequest {

    @NotBlank
    @Size(max = 150)
    private String name;

    @NotNull
    private OrganizationType type;

    @Size(max = 255)
    private String address;

    @Email
    @Size(max = 100)
    private String contactEmail;

    @Size(max = 50)
    private String contactPhone;
}
