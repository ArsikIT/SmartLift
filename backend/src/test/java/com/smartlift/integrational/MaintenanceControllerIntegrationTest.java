package com.smartlift.integrational;

import com.jayway.jsonpath.JsonPath;
import com.smartlift.dto.request.LiftRequest;
import com.smartlift.dto.request.MaintenanceRequest;
import com.smartlift.dto.request.UserRequest;
import com.smartlift.dto.response.UserResponse;
import com.smartlift.model.enums.MaintenanceStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MaintenanceControllerIntegrationTest extends BaseIntegrationTest {

    private record OrgContext(String token, Long orgId, Integer liftId, Long userId) {}

    private OrgContext setupOrgWithLift(String prefix, String orgType) throws Exception {
        UserResponse admin = registerOrganization(prefix + "_admin", prefix + "@test.com",
                "password123", prefix + "Org", orgType);
        String token = login(prefix + "_admin", "password123");

        LiftRequest liftReq = new LiftRequest();
        liftReq.setSerialNumber(prefix + "-SERIAL");
        liftReq.setModel("TestModel");
        liftReq.setManufacturer("Mfg");
        if ("MANUFACTURER".equals(orgType)) {
            liftReq.setManufacturerOrganizationId(admin.getOrganization().getId());
        } else if ("SERVICE".equals(orgType)) {
            liftReq.setServiceOrganizationId(admin.getOrganization().getId());
        } else {
            liftReq.setManagementOrganizationId(admin.getOrganization().getId());
        }

        MvcResult liftResult = mockMvc.perform(post("/api/lifts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(liftReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Integer liftId = JsonPath.read(liftResult.getResponse().getContentAsString(), "$.id");
        return new OrgContext(token, admin.getOrganization().getId(), liftId, admin.getId());
    }

    private UserResponse createOrgUser(String token, String username, String email) throws Exception {
        UserRequest request = new UserRequest();
        request.setUsername(username);
        request.setEmail(email);
        request.setPassword("password123");

        MvcResult result = mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readValue(result.getResponse().getContentAsString(), UserResponse.class);
    }

    @Test
    void createMaintenance_returns201() throws Exception {
        OrgContext ctx = setupOrgWithLift("mntcreate", "MANUFACTURER");

        MaintenanceRequest request = new MaintenanceRequest();
        request.setLiftId(ctx.liftId.longValue());
        request.setTitle("Routine check");
        request.setDescription("Monthly inspection");

        mockMvc.perform(post("/api/maintenances")
                        .header("Authorization", "Bearer " + ctx.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Routine check"))
                .andExpect(jsonPath("$.description").value("Monthly inspection"));
    }

    @Test
    void getAllMaintenances_returnsPage() throws Exception {
        OrgContext ctx = setupOrgWithLift("mntlist", "MANUFACTURER");

        MaintenanceRequest request = new MaintenanceRequest();
        request.setLiftId(ctx.liftId.longValue());
        request.setTitle("Check brakes");

        mockMvc.perform(post("/api/maintenances")
                        .header("Authorization", "Bearer " + ctx.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/maintenances")
                        .header("Authorization", "Bearer " + ctx.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getMaintenancesByLiftId_filtersCorrectly() throws Exception {
        OrgContext ctx = setupOrgWithLift("mntfilter", "MANUFACTURER");

        MaintenanceRequest request = new MaintenanceRequest();
        request.setLiftId(ctx.liftId.longValue());
        request.setTitle("Filtered maintenance");

        mockMvc.perform(post("/api/maintenances")
                        .header("Authorization", "Bearer " + ctx.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/maintenances")
                        .param("liftId", ctx.liftId.toString())
                        .header("Authorization", "Bearer " + ctx.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void updateMaintenance_changesStatus() throws Exception {
        OrgContext ctx = setupOrgWithLift("mntupd", "SERVICE");
        UserResponse technician = createOrgUser(ctx.token, "mntupd_tech", "mntupd_tech@test.com");

        MaintenanceRequest createReq = new MaintenanceRequest();
        createReq.setLiftId(ctx.liftId.longValue());
        createReq.setTitle("Status transition test");

        MvcResult createResult = mockMvc.perform(post("/api/maintenances")
                        .header("Authorization", "Bearer " + ctx.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Integer mntId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");

        MaintenanceRequest updateReq = new MaintenanceRequest();
        updateReq.setLiftId(ctx.liftId.longValue());
        updateReq.setTitle("Status transition test");
        updateReq.setStatus(MaintenanceStatus.IN_PROGRESS);
        updateReq.setAssignedTechnicianId(technician.getId());

        mockMvc.perform(put("/api/maintenances/" + mntId)
                        .header("Authorization", "Bearer " + ctx.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void updateMaintenance_returns400WhenStartingWithoutAssignedTechnician() throws Exception {
        OrgContext ctx = setupOrgWithLift("mntstart", "SERVICE");

        MaintenanceRequest createReq = new MaintenanceRequest();
        createReq.setLiftId(ctx.liftId.longValue());
        createReq.setTitle("Workflow validation");

        MvcResult createResult = mockMvc.perform(post("/api/maintenances")
                        .header("Authorization", "Bearer " + ctx.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Integer mntId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");

        MaintenanceRequest updateReq = new MaintenanceRequest();
        updateReq.setLiftId(ctx.liftId.longValue());
        updateReq.setTitle("Workflow validation");
        updateReq.setStatus(MaintenanceStatus.IN_PROGRESS);

        mockMvc.perform(put("/api/maintenances/" + mntId)
                        .header("Authorization", "Bearer " + ctx.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Assigned technician is required to start maintenance"));
    }

    @Test
    void updateMaintenance_returns403WhenCompletedByNonAssignedTechnician() throws Exception {
        OrgContext ctx = setupOrgWithLift("mntdone", "SERVICE");
        UserResponse technician = createOrgUser(ctx.token, "mntdone_tech", "mntdone_tech@test.com");

        MaintenanceRequest createReq = new MaintenanceRequest();
        createReq.setLiftId(ctx.liftId.longValue());
        createReq.setTitle("Completion ownership");

        MvcResult createResult = mockMvc.perform(post("/api/maintenances")
                        .header("Authorization", "Bearer " + ctx.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Integer mntId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");

        MaintenanceRequest startReq = new MaintenanceRequest();
        startReq.setLiftId(ctx.liftId.longValue());
        startReq.setTitle("Completion ownership");
        startReq.setStatus(MaintenanceStatus.IN_PROGRESS);
        startReq.setAssignedTechnicianId(technician.getId());

        mockMvc.perform(put("/api/maintenances/" + mntId)
                        .header("Authorization", "Bearer " + ctx.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(startReq)))
                .andExpect(status().isOk());

        MaintenanceRequest doneReq = new MaintenanceRequest();
        doneReq.setLiftId(ctx.liftId.longValue());
        doneReq.setTitle("Completion ownership");
        doneReq.setStatus(MaintenanceStatus.DONE);

        mockMvc.perform(put("/api/maintenances/" + mntId)
                        .header("Authorization", "Bearer " + ctx.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(doneReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteMaintenance_returns204() throws Exception {
        OrgContext ctx = setupOrgWithLift("mntdel", "MANUFACTURER");

        MaintenanceRequest request = new MaintenanceRequest();
        request.setLiftId(ctx.liftId.longValue());
        request.setTitle("To be deleted");

        MvcResult createResult = mockMvc.perform(post("/api/maintenances")
                        .header("Authorization", "Bearer " + ctx.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        Integer mntId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(delete("/api/maintenances/" + mntId)
                        .header("Authorization", "Bearer " + ctx.token))
                .andExpect(status().isNoContent());
    }
}
