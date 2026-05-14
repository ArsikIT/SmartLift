package com.smartlift.integrational;

import com.smartlift.dto.request.OrganizationRequest;
import com.smartlift.model.enums.OrganizationType;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OrganizationControllerIntegrationTest extends BaseIntegrationTest {

    @Test
    void getMyOrganization_returnsCurrentUserOrg() throws Exception {
        TestAccount admin = testAccount("orgme_admin", "orgme@test.com");
        String token = registerAndLogin(admin, "OrgMeOrg", "MANUFACTURER");

        mockMvc.perform(get("/api/organizations/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("OrgMeOrg"))
                .andExpect(jsonPath("$.type").value("MANUFACTURER"));
    }

    @Test
    void updateMyOrganization_returns200() throws Exception {
        TestAccount admin = testAccount("orgupd_admin", "orgupd@test.com");
        String token = registerAndLogin(admin, "OrgUpdOrg", "SERVICE");

        OrganizationRequest request = new OrganizationRequest();
        request.setName("OrgUpdOrg");
        request.setType(OrganizationType.SERVICE);
        request.setAddress("123 Main St");
        request.setContactEmail("contact@orgupd.com");
        request.setContactPhone("+1234567890");

        mockMvc.perform(put("/api/organizations/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address").value("123 Main St"))
                .andExpect(jsonPath("$.contactEmail").value("contact@orgupd.com"))
                .andExpect(jsonPath("$.contactPhone").value("+1234567890"));
    }

    @Test
    void nonAdminUser_cannotAccessOrgEndpoints() throws Exception {
        TestAccount admin = testAccount("orgrole_admin", "orgrole@test.com");
        String adminToken = registerAndLogin(admin, "OrgRoleOrg", "MANAGEMENT");

        TestAccount regularUser = testAccount("org_regular_user", "orgregular@test.com");
        com.smartlift.dto.request.UserRequest userReq = newUserRequest(regularUser);

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/users")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(userReq)))
                .andExpect(status().isCreated());

        String regularToken = login(regularUser);

        mockMvc.perform(get("/api/organizations/me")
                        .header("Authorization", "Bearer " + regularToken))
                .andExpect(status().isForbidden());
    }
}
