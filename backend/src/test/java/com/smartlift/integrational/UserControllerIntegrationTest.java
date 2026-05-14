package com.smartlift.integrational;

import com.smartlift.dto.request.UserRequest;
import com.smartlift.dto.response.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserControllerIntegrationTest extends BaseIntegrationTest {

    @Test
    void getAllUsers_returnsPageOfUsersInSameOrg() throws Exception {
        TestAccount admin = testAccount("usrlist_admin", "usrlist@test.com");
        String token = registerAndLogin(admin, "UsrListOrg", "MANUFACTURER");

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].username").value("usrlist_admin"));
    }

    @Test
    void createUser_returns201() throws Exception {
        TestAccount admin = testAccount("usrcreate_admin", "usrcreate@test.com");
        String token = registerAndLogin(admin, "UsrCreateOrg", "SERVICE");

        TestAccount employee = testAccount("new_employee", "employee@test.com");
        UserRequest request = newUserRequest(employee);

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.username").value("new_employee"))
                .andExpect(jsonPath("$.email").value("employee@test.com"));
    }

    @Test
    void getUserById_returnsUser() throws Exception {
        TestAccount admin = testAccount("usrget_admin", "usrget@test.com");
        String token = registerAndLogin(admin, "UsrGetOrg", "MANAGEMENT");

        TestAccount targetUser = testAccount("getme_user", "getme@test.com");
        UserRequest request = newUserRequest(targetUser);

        MvcResult createResult = mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        UserResponse created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), UserResponse.class);

        mockMvc.perform(get("/api/users/" + created.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("getme_user"));
    }

    @Test
    void updateUser_returns200() throws Exception {
        TestAccount admin = testAccount("usrupd_admin", "usrupd@test.com");
        String token = registerAndLogin(admin, "UsrUpdOrg", "MANUFACTURER");

        TestAccount createdUser = testAccount("upd_target", "updtarget@test.com");
        UserRequest createReq = newUserRequest(createdUser);

        MvcResult createResult = mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        UserResponse created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), UserResponse.class);

        TestAccount updatedUser = testAccount("upd_target_renamed", "updrenamed@test.com");
        UserRequest updateReq = newUpdatedUserRequest(updatedUser);

        mockMvc.perform(put("/api/users/" + created.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("upd_target_renamed"))
                .andExpect(jsonPath("$.email").value("updrenamed@test.com"));
    }

    @Test
    void deleteUser_returns204() throws Exception {
        TestAccount admin = testAccount("usrdel_admin", "usrdel@test.com");
        String token = registerAndLogin(admin, "UsrDelOrg", "SERVICE");

        TestAccount targetUser = testAccount("del_target", "deltarget@test.com");
        UserRequest request = newUserRequest(targetUser);

        MvcResult createResult = mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        UserResponse created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), UserResponse.class);

        mockMvc.perform(delete("/api/users/" + created.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/" + created.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUser_returns400WhenDeletingSelf() throws Exception {
        TestAccount adminAccount = testAccount("selfdelete_admin", "selfdelete@test.com");
        UserResponse admin = registerOrganization(adminAccount, "SelfDelOrg", "MANUFACTURER");
        String token = login(adminAccount);

        mockMvc.perform(delete("/api/users/" + admin.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonAdminUser_returns403ForUserEndpoints() throws Exception {
        TestAccount admin = testAccount("nonadmin_org_admin", "nonadmin_org@test.com");
        String adminToken = registerAndLogin(admin, "NonAdminOrg", "SERVICE");

        TestAccount regularUser = testAccount("regular_user", "regular@test.com");
        UserRequest request = newUserRequest(regularUser);

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        String regularToken = login(regularUser);

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + regularToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void orgBoundary_cannotSeeUsersFromOtherOrg() throws Exception {
        TestAccount org1 = testAccount("orgbound_admin1", "orgbound1@test.com");
        TestAccount org2 = testAccount("orgbound_admin2", "orgbound2@test.com");
        String tokenOrg1 = registerAndLogin(org1, "OrgBound1", "MANUFACTURER");
        String tokenOrg2 = registerAndLogin(org2, "OrgBound2", "SERVICE");

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + tokenOrg1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("orgbound_admin1"))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + tokenOrg2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("orgbound_admin2"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
