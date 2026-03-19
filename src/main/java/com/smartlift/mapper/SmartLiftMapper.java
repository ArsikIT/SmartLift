package com.smartlift.mapper;

import com.smartlift.dto.LiftEventResponse;
import com.smartlift.dto.LiftResponse;
import com.smartlift.dto.OrganizationSummaryResponse;
import com.smartlift.dto.UserSummaryResponse;
import com.smartlift.model.Lift;
import com.smartlift.model.LiftEvent;
import com.smartlift.model.Organization;
import com.smartlift.model.User;

public final class SmartLiftMapper {

    private SmartLiftMapper() {
    }

    public static LiftResponse toLiftResponse(Lift lift) {
        return LiftResponse.builder()
                .id(lift.getId())
                .serialNumber(lift.getSerialNumber())
                .model(lift.getModel())
                .manufacturer(lift.getManufacturer())
                .status(lift.getStatus())
                .manufacturerOrganization(toOrganizationSummary(lift.getManufacturerOrganization()))
                .serviceOrganization(toOrganizationSummary(lift.getServiceOrganization()))
                .managementOrganization(toOrganizationSummary(lift.getManagementOrganization()))
                .createdAt(lift.getCreatedAt())
                .updatedAt(lift.getUpdatedAt())
                .build();
    }

    public static LiftEventResponse toLiftEventResponse(LiftEvent event) {
        return LiftEventResponse.builder()
                .id(event.getId())
                .liftId(event.getLift().getId())
                .liftSerialNumber(event.getLift().getSerialNumber())
                .type(event.getType())
                .eventAt(event.getEventAt())
                .description(event.getDescription())
                .performedBy(toUserSummary(event.getPerformedBy()))
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }

    private static OrganizationSummaryResponse toOrganizationSummary(Organization organization) {
        if (organization == null) {
            return null;
        }
        return OrganizationSummaryResponse.builder()
                .id(organization.getId())
                .name(organization.getName())
                .type(organization.getType())
                .build();
    }

    private static UserSummaryResponse toUserSummary(User user) {
        if (user == null) {
            return null;
        }
        return UserSummaryResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }
}
