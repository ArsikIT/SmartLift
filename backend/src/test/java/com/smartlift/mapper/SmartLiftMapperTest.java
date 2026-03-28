package com.smartlift.mapper;

import com.smartlift.dto.response.DocumentResponse;
import com.smartlift.dto.response.LiftEventResponse;
import com.smartlift.dto.response.LiftResponse;
import com.smartlift.dto.response.MaintenanceResponse;
import com.smartlift.dto.response.OrganizationResponse;
import com.smartlift.dto.response.UserResponse;
import com.smartlift.model.Document;
import com.smartlift.model.Lift;
import com.smartlift.model.LiftEvent;
import com.smartlift.model.Maintenance;
import com.smartlift.model.Organization;
import com.smartlift.model.Role;
import com.smartlift.model.User;
import com.smartlift.model.enums.LiftEventType;
import com.smartlift.model.enums.LiftStatus;
import com.smartlift.model.enums.MaintenanceStatus;
import com.smartlift.model.enums.OrganizationType;
import com.smartlift.model.enums.RoleName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SmartLiftMapperTest {

    private final LocalDateTime now = LocalDateTime.now();

    @Test
    void toLiftResponse_mapsAllFields() {
        Organization mfg = createOrganization(1L, "MfgOrg", OrganizationType.MANUFACTURER);
        Organization svc = createOrganization(2L, "SvcOrg", OrganizationType.SERVICE);
        Organization mgmt = createOrganization(3L, "MgmtOrg", OrganizationType.MANAGEMENT);

        Lift lift = new Lift();
        lift.setId(10L);
        lift.setSerialNumber("SN-001");
        lift.setModel("ModelX");
        lift.setManufacturer("Acme");
        lift.setStatus(LiftStatus.ACTIVE);
        lift.setManufacturerOrganization(mfg);
        lift.setServiceOrganization(svc);
        lift.setManagementOrganization(mgmt);
        lift.setCreatedAt(now);
        lift.setUpdatedAt(now);

        LiftResponse response = SmartLiftMapper.toLiftResponse(lift);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getSerialNumber()).isEqualTo("SN-001");
        assertThat(response.getModel()).isEqualTo("ModelX");
        assertThat(response.getManufacturer()).isEqualTo("Acme");
        assertThat(response.getStatus()).isEqualTo(LiftStatus.ACTIVE);
        assertThat(response.getManufacturerOrganization().getId()).isEqualTo(1L);
        assertThat(response.getManufacturerOrganization().getName()).isEqualTo("MfgOrg");
        assertThat(response.getServiceOrganization().getId()).isEqualTo(2L);
        assertThat(response.getManagementOrganization().getId()).isEqualTo(3L);
        assertThat(response.getCreatedAt()).isEqualTo(now);
        assertThat(response.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    void toLiftResponse_handlesNullOrganizations() {
        Lift lift = new Lift();
        lift.setId(10L);
        lift.setSerialNumber("SN-001");
        lift.setModel("ModelX");
        lift.setStatus(LiftStatus.CREATED);
        lift.setCreatedAt(now);
        lift.setUpdatedAt(now);

        LiftResponse response = SmartLiftMapper.toLiftResponse(lift);

        assertThat(response.getManufacturerOrganization()).isNull();
        assertThat(response.getServiceOrganization()).isNull();
        assertThat(response.getManagementOrganization()).isNull();
    }

    @Test
    void toLiftEventResponse_mapsAllFields() {
        Lift lift = new Lift();
        lift.setId(5L);
        lift.setSerialNumber("SN-002");

        User performer = createUser(7L, "techUser", "tech@test.com");

        LiftEvent event = new LiftEvent();
        event.setId(20L);
        event.setLift(lift);
        event.setType(LiftEventType.FAULT);
        event.setEventAt(now);
        event.setDescription("Motor failure");
        event.setPerformedBy(performer);
        event.setCreatedAt(now);
        event.setUpdatedAt(now);

        LiftEventResponse response = SmartLiftMapper.toLiftEventResponse(event);

        assertThat(response.getId()).isEqualTo(20L);
        assertThat(response.getLiftId()).isEqualTo(5L);
        assertThat(response.getLiftSerialNumber()).isEqualTo("SN-002");
        assertThat(response.getType()).isEqualTo(LiftEventType.FAULT);
        assertThat(response.getEventAt()).isEqualTo(now);
        assertThat(response.getDescription()).isEqualTo("Motor failure");
        assertThat(response.getPerformedBy().getId()).isEqualTo(7L);
        assertThat(response.getPerformedBy().getUsername()).isEqualTo("techUser");
    }

    @Test
    void toLiftEventResponse_handlesNullPerformedBy() {
        Lift lift = new Lift();
        lift.setId(5L);
        lift.setSerialNumber("SN-002");

        LiftEvent event = new LiftEvent();
        event.setId(20L);
        event.setLift(lift);
        event.setType(LiftEventType.CREATED);
        event.setEventAt(now);
        event.setDescription("Created");
        event.setPerformedBy(null);
        event.setCreatedAt(now);
        event.setUpdatedAt(now);

        LiftEventResponse response = SmartLiftMapper.toLiftEventResponse(event);
        assertThat(response.getPerformedBy()).isNull();
    }

    @Test
    void toOrganizationResponse_mapsAllFields() {
        Organization org = createOrganization(1L, "TestOrg", OrganizationType.SERVICE);
        org.setAddress("123 Main St");
        org.setContactEmail("contact@test.com");
        org.setContactPhone("+1234567890");
        org.setCreatedAt(now);
        org.setUpdatedAt(now);

        OrganizationResponse response = SmartLiftMapper.toOrganizationResponse(org);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("TestOrg");
        assertThat(response.getType()).isEqualTo(OrganizationType.SERVICE);
        assertThat(response.getAddress()).isEqualTo("123 Main St");
        assertThat(response.getContactEmail()).isEqualTo("contact@test.com");
        assertThat(response.getContactPhone()).isEqualTo("+1234567890");
    }

    @Test
    void toUserResponse_mapsAllFieldsIncludingRoles() {
        Organization org = createOrganization(1L, "TestOrg", OrganizationType.MANUFACTURER);

        Role adminRole = new Role();
        adminRole.setId(1L);
        adminRole.setName(RoleName.ADMIN);

        Role mfgRole = new Role();
        mfgRole.setId(2L);
        mfgRole.setName(RoleName.MANUFACTURER);

        User user = new User();
        user.setId(5L);
        user.setUsername("admin1");
        user.setEmail("admin@test.com");
        user.setEnabled(true);
        user.setOrganization(org);
        user.setRoles(Set.of(adminRole, mfgRole));
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        UserResponse response = SmartLiftMapper.toUserResponse(user);

        assertThat(response.getId()).isEqualTo(5L);
        assertThat(response.getUsername()).isEqualTo("admin1");
        assertThat(response.getEmail()).isEqualTo("admin@test.com");
        assertThat(response.isEnabled()).isTrue();
        assertThat(response.getOrganization().getId()).isEqualTo(1L);
        assertThat(response.getRoles()).containsExactlyInAnyOrder("ADMIN", "MANUFACTURER");
    }

    @Test
    void toMaintenanceResponse_mapsAllFields() {
        Lift lift = new Lift();
        lift.setId(3L);
        lift.setSerialNumber("SN-003");

        User tech = createUser(10L, "tech", "tech@test.com");
        User requester = createUser(11L, "requester", "req@test.com");

        Maintenance m = new Maintenance();
        m.setId(30L);
        m.setLift(lift);
        m.setTitle("Annual check");
        m.setDescription("Yearly inspection");
        m.setStatus(MaintenanceStatus.IN_PROGRESS);
        m.setAssignedTechnician(tech);
        m.setRequestedBy(requester);
        m.setRequestedAt(now.minusDays(2));
        m.setStartedAt(now.minusDays(1));
        m.setCompletedAt(null);
        m.setCreatedAt(now);
        m.setUpdatedAt(now);

        MaintenanceResponse response = SmartLiftMapper.toMaintenanceResponse(m);

        assertThat(response.getId()).isEqualTo(30L);
        assertThat(response.getLiftId()).isEqualTo(3L);
        assertThat(response.getLiftSerialNumber()).isEqualTo("SN-003");
        assertThat(response.getTitle()).isEqualTo("Annual check");
        assertThat(response.getDescription()).isEqualTo("Yearly inspection");
        assertThat(response.getStatus()).isEqualTo(MaintenanceStatus.IN_PROGRESS);
        assertThat(response.getAssignedTechnician().getId()).isEqualTo(10L);
        assertThat(response.getRequestedBy().getId()).isEqualTo(11L);
        assertThat(response.getRequestedAt()).isEqualTo(now.minusDays(2));
        assertThat(response.getStartedAt()).isEqualTo(now.minusDays(1));
        assertThat(response.getCompletedAt()).isNull();
    }

    @Test
    void toDocumentResponse_mapsAllFieldsWithLiftAndMaintenance() {
        Lift lift = new Lift();
        lift.setId(4L);

        Maintenance maintenance = new Maintenance();
        maintenance.setId(40L);

        User uploader = createUser(12L, "uploader", "up@test.com");

        Document doc = new Document();
        doc.setId(50L);
        doc.setFileName("report.pdf");
        doc.setFilePath("/docs/report.pdf");
        doc.setContentType("application/pdf");
        doc.setLift(lift);
        doc.setMaintenance(maintenance);
        doc.setUploadedBy(uploader);
        doc.setCreatedAt(now);
        doc.setUpdatedAt(now);

        DocumentResponse response = SmartLiftMapper.toDocumentResponse(doc);

        assertThat(response.getId()).isEqualTo(50L);
        assertThat(response.getFileName()).isEqualTo("report.pdf");
        assertThat(response.getFilePath()).isEqualTo("/docs/report.pdf");
        assertThat(response.getContentType()).isEqualTo("application/pdf");
        assertThat(response.getLiftId()).isEqualTo(4L);
        assertThat(response.getMaintenanceId()).isEqualTo(40L);
        assertThat(response.getUploadedBy().getId()).isEqualTo(12L);
    }

    @Test
    void toDocumentResponse_handlesNullLiftAndMaintenance() {
        Document doc = new Document();
        doc.setId(50L);
        doc.setFileName("orphan.pdf");
        doc.setFilePath("/docs/orphan.pdf");
        doc.setLift(null);
        doc.setMaintenance(null);
        doc.setUploadedBy(null);
        doc.setCreatedAt(now);
        doc.setUpdatedAt(now);

        DocumentResponse response = SmartLiftMapper.toDocumentResponse(doc);

        assertThat(response.getLiftId()).isNull();
        assertThat(response.getMaintenanceId()).isNull();
        assertThat(response.getUploadedBy()).isNull();
    }

    private Organization createOrganization(Long id, String name, OrganizationType type) {
        Organization org = new Organization();
        org.setId(id);
        org.setName(name);
        org.setType(type);
        return org;
    }

    private User createUser(Long id, String username, String email) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        return user;
    }
}
