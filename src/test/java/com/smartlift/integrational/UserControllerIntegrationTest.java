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
        String token = registerAndLogin("usrlist_admin", "usrlist@test.com",
                "password123", "UsrListOrg", "MANUFACTURER");

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].username").value("usrlist_admin"));
    }

    @Test
    void createUser_returns201() throws Exception {
        String token = registerAndLogin("usrcreate_admin", "usrcreate@test.com",
                "password123", "UsrCreateOrg", "SERVICE");

        UserRequest request = new UserRequest();
        request.setUsername("new_employee");
        request.setEmail("employee@test.com");
        request.setPassword("password123");
        request.setEnabled(true);

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
        String token = registerAndLogin("usrget_admin", "usrget@test.com",
                "password123", "UsrGetOrg", "MANAGEMENT");

        UserRequest request = new UserRequest();
        request.setUsername("getme_user");
        request.setEmail("getme@test.com");
        request.setPassword("password123");
        request.setEnabled(true);

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
        String token = registerAndLogin("usrupd_admin", "usrupd@test.com",
                "password123", "UsrUpdOrg", "MANUFACTURER");

        UserRequest createReq = new UserRequest();
        createReq.setUsername("upd_target");
        createReq.setEmail("updtarget@test.com");
        createReq.setPassword("password123");
        createReq.setEnabled(true);

        MvcResult createResult = mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        UserResponse created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), UserResponse.class);

        UserRequest updateReq = new UserRequest();
        updateReq.setUsername("upd_target_renamed");
        updateReq.setEmail("updrenamed@test.com");
        updateReq.setPassword("newpassword123");
        updateReq.setEnabled(true);

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
        String token = registerAndLogin("usrdel_admin", "usrdel@test.com",
                "password123", "UsrDelOrg", "SERVICE");

        UserRequest request = new UserRequest();
        request.setUsername("del_target");
        request.setEmail("deltarget@test.com");
        request.setPassword("password123");
        request.setEnabled(true);

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
        UserResponse admin = registerOrganization("selfdelete_admin", "selfdelete@test.com",
                "password123", "SelfDelOrg", "MANUFACTURER");
        String token = login("selfdelete_admin", "password123");

        mockMvc.perform(delete("/api/users/" + admin.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonAdminUser_returns403ForUserEndpoints() throws Exception {
        String adminToken = registerAndLogin("nonadmin_org_admin", "nonadmin_org@test.com",
                "password123", "NonAdminOrg", "SERVICE");

        UserRequest request = new UserRequest();
        request.setUsername("regular_user");
        request.setEmail("regular@test.com");
        request.setPassword("password123");
        request.setEnabled(true);

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        String regularToken = login("regular_user", "password123");

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + regularToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void orgBoundary_cannotSeeUsersFromOtherOrg() throws Exception {
        String tokenOrg1 = registerAndLogin("orgbound_admin1", "orgbound1@test.com",
                "password123", "OrgBound1", "MANUFACTURER");
        String tokenOrg2 = registerAndLogin("orgbound_admin2", "orgbound2@test.com",
                "password123", "OrgBound2", "SERVICE");

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
