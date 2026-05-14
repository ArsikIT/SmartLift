package com.smartlift.integrational;

import com.jayway.jsonpath.JsonPath;
import com.smartlift.dto.request.LiftRequest;
import com.smartlift.dto.response.UserResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("smoke")
class BackendSmokeIntegrationTest extends BaseIntegrationTest {

    @Test
    void smoke_registerAndLogin_returnsJwtToken() throws Exception {
        TestAccount account = testAccount("smoke_admin", "smoke_admin@test.com");
        var registerRequest = newRegisterRequest(account, "SmokeOrg", "MANUFACTURER");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.username").value("smoke_admin"))
                .andExpect(jsonPath("$.organization.name").value("SmokeOrg"));

        var loginRequest = newLoginRequest(account);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("smoke_admin"));
    }

    @Test
    void smoke_jwtProtectedEndpoint_returns200WithValidToken() throws Exception {
        TestAccount account = testAccount("smoke_jwt_admin", "smoke_jwt@test.com");
        String token = registerAndLogin(account, "SmokeJwtOrg", "MANUFACTURER");

        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void smoke_liftCrud_roundTripSucceeds() throws Exception {
        TestAccount account = testAccount("smoke_lift_admin", "smoke_lift@test.com");
        UserResponse admin = registerOrganization(account, "SmokeLiftOrg", "MANUFACTURER");
        String token = login(account);

        LiftRequest createRequest = new LiftRequest();
        createRequest.setSerialNumber("SMOKE-LIFT-001");
        createRequest.setModel("Smoke Model");
        createRequest.setManufacturer("Smoke Mfg");
        createRequest.setManufacturerOrganizationId(admin.getOrganization().getId());

        MvcResult createResult = mockMvc.perform(post("/api/lifts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.serialNumber").value("SMOKE-LIFT-001"))
                .andReturn();

        Integer liftId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/lifts/" + liftId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.model").value("Smoke Model"));

        LiftRequest updateRequest = new LiftRequest();
        updateRequest.setSerialNumber("SMOKE-LIFT-001");
        updateRequest.setModel("Smoke Model Updated");
        updateRequest.setManufacturer("Smoke Mfg Updated");
        updateRequest.setManufacturerOrganizationId(admin.getOrganization().getId());

        mockMvc.perform(put("/api/lifts/" + liftId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.model").value("Smoke Model Updated"))
                .andExpect(jsonPath("$.manufacturer").value("Smoke Mfg Updated"));

        mockMvc.perform(delete("/api/lifts/" + liftId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }
}
