package com.smartlift.integrational;

import com.jayway.jsonpath.JsonPath;
import com.smartlift.dto.request.LiftRequest;
import com.smartlift.dto.response.UserResponse;
import com.smartlift.model.enums.LiftStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LiftControllerIntegrationTest extends BaseIntegrationTest {

    @Test
    void createLift_returns201() throws Exception {
        TestAccount account = testAccount("liftcreate_admin", "liftcreate@test.com");
        UserResponse admin = registerOrganization(account, "LiftCreateOrg", "MANUFACTURER");
        String token = login(account);

        LiftRequest request = new LiftRequest();
        request.setSerialNumber("LIFT-001");
        request.setModel("Express 3000");
        request.setManufacturer("LiftCorp");
        request.setStatus(LiftStatus.CREATED);
        request.setManufacturerOrganizationId(admin.getOrganization().getId());

        mockMvc.perform(post("/api/lifts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.serialNumber").value("LIFT-001"))
                .andExpect(jsonPath("$.model").value("Express 3000"))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void getAllLifts_returnsLiftsForCurrentOrg() throws Exception {
        TestAccount account = testAccount("liftlist_admin", "liftlist@test.com");
        UserResponse admin = registerOrganization(account, "LiftListOrg", "MANUFACTURER");
        String token = login(account);

        LiftRequest request = new LiftRequest();
        request.setSerialNumber("LIFT-LIST-001");
        request.setModel("Model A");
        request.setManufacturer("Mfg");
        request.setManufacturerOrganizationId(admin.getOrganization().getId());

        mockMvc.perform(post("/api/lifts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/lifts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getLiftById_returnsLift() throws Exception {
        TestAccount account = testAccount("liftget_admin", "liftget@test.com");
        UserResponse admin = registerOrganization(account, "LiftGetOrg", "MANUFACTURER");
        String token = login(account);

        LiftRequest request = new LiftRequest();
        request.setSerialNumber("LIFT-GET-001");
        request.setModel("GetModel");
        request.setManufacturer("Mfg");
        request.setManufacturerOrganizationId(admin.getOrganization().getId());

        MvcResult createResult = mockMvc.perform(post("/api/lifts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        Integer liftId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/lifts/" + liftId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serialNumber").value("LIFT-GET-001"));
    }

    @Test
    void updateLift_returns200() throws Exception {
        TestAccount account = testAccount("liftupd_admin", "liftupd@test.com");
        UserResponse admin = registerOrganization(account, "LiftUpdOrg", "MANUFACTURER");
        String token = login(account);

        LiftRequest createReq = new LiftRequest();
        createReq.setSerialNumber("LIFT-UPD-001");
        createReq.setModel("OldModel");
        createReq.setManufacturer("Mfg");
        createReq.setManufacturerOrganizationId(admin.getOrganization().getId());

        MvcResult createResult = mockMvc.perform(post("/api/lifts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Integer liftId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");

        LiftRequest updateReq = new LiftRequest();
        updateReq.setSerialNumber("LIFT-UPD-001");
        updateReq.setModel("NewModel");
        updateReq.setManufacturer("NewMfg");
        updateReq.setManufacturerOrganizationId(admin.getOrganization().getId());

        mockMvc.perform(put("/api/lifts/" + liftId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.model").value("NewModel"))
                .andExpect(jsonPath("$.manufacturer").value("NewMfg"));
    }

    @Test
    void deleteLift_returns204() throws Exception {
        TestAccount account = testAccount("liftdel_admin", "liftdel@test.com");
        UserResponse admin = registerOrganization(account, "LiftDelOrg", "MANUFACTURER");
        String token = login(account);

        LiftRequest request = new LiftRequest();
        request.setSerialNumber("LIFT-DEL-001");
        request.setModel("DeleteMe");
        request.setManufacturer("Mfg");
        request.setManufacturerOrganizationId(admin.getOrganization().getId());

        MvcResult createResult = mockMvc.perform(post("/api/lifts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        Integer liftId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(delete("/api/lifts/" + liftId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void createLift_returns409WhenDuplicateSerialNumber() throws Exception {
        TestAccount account = testAccount("liftdup_admin", "liftdup@test.com");
        UserResponse admin = registerOrganization(account, "LiftDupOrg", "MANUFACTURER");
        String token = login(account);

        LiftRequest request = new LiftRequest();
        request.setSerialNumber("LIFT-DUP-001");
        request.setModel("Model");
        request.setManufacturer("Mfg");
        request.setManufacturerOrganizationId(admin.getOrganization().getId());

        mockMvc.perform(post("/api/lifts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/lifts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void serviceRole_cannotCreateLift() throws Exception {
        TestAccount adminAccount = testAccount("liftrole_admin", "liftrole@test.com");
        String adminToken = registerAndLogin(adminAccount, "LiftRoleOrg", "SERVICE");

        TestAccount serviceUser = testAccount("service_only_user", "serviceonly@test.com");
        com.smartlift.dto.request.UserRequest userReq = newUserRequest(serviceUser);

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userReq)))
                .andExpect(status().isCreated());

        String serviceToken = login(serviceUser);

        LiftRequest request = new LiftRequest();
        request.setSerialNumber("LIFT-NOROLE");
        request.setModel("Model");
        request.setManufacturer("Mfg");

        mockMvc.perform(post("/api/lifts")
                        .header("Authorization", "Bearer " + serviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
