package com.smartlift.mapper;

import com.smartlift.dto.DocumentResponse;
import com.smartlift.dto.LiftEventResponse;
import com.smartlift.dto.LiftResponse;
import com.smartlift.dto.MaintenanceResponse;
import com.smartlift.dto.OrganizationResponse;
import com.smartlift.dto.OrganizationSummaryResponse;
import com.smartlift.dto.UserResponse;
import com.smartlift.dto.UserSummaryResponse;
import com.smartlift.model.Document;
import com.smartlift.model.Lift;
import com.smartlift.model.LiftEvent;
import com.smartlift.model.Maintenance;
import com.smartlift.model.Organization;
import com.smartlift.model.Role;
import com.smartlift.model.User;
import java.util.Set;
import java.util.stream.Collectors;

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

    public static OrganizationResponse toOrganizationResponse(Organization org) {
        return OrganizationResponse.builder()
                .id(org.getId())
                .name(org.getName())
                .type(org.getType())
                .address(org.getAddress())
                .contactEmail(org.getContactEmail())
                .contactPhone(org.getContactPhone())
                .createdAt(org.getCreatedAt())
                .updatedAt(org.getUpdatedAt())
                .build();
    }

    public static UserResponse toUserResponse(User user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .map(Enum::name)
                .collect(Collectors.toSet());

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .enabled(user.isEnabled())
                .organization(toOrganizationSummary(user.getOrganization()))
                .roles(roleNames)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    public static MaintenanceResponse toMaintenanceResponse(Maintenance m) {
        return MaintenanceResponse.builder()
                .id(m.getId())
                .liftId(m.getLift().getId())
                .liftSerialNumber(m.getLift().getSerialNumber())
                .title(m.getTitle())
                .description(m.getDescription())
                .status(m.getStatus())
                .assignedTechnician(toUserSummary(m.getAssignedTechnician()))
                .requestedBy(toUserSummary(m.getRequestedBy()))
                .requestedAt(m.getRequestedAt())
                .startedAt(m.getStartedAt())
                .completedAt(m.getCompletedAt())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .build();
    }

    public static DocumentResponse toDocumentResponse(Document doc) {
        return DocumentResponse.builder()
                .id(doc.getId())
                .fileName(doc.getFileName())
                .filePath(doc.getFilePath())
                .contentType(doc.getContentType())
                .liftId(doc.getLift() != null ? doc.getLift().getId() : null)
                .maintenanceId(doc.getMaintenance() != null ? doc.getMaintenance().getId() : null)
                .uploadedBy(toUserSummary(doc.getUploadedBy()))
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
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
