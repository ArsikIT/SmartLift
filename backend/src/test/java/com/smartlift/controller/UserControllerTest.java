package com.smartlift.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlift.dto.request.UserRequest;
import com.smartlift.dto.response.OrganizationSummaryResponse;
import com.smartlift.dto.response.UserResponse;
import com.smartlift.exception.BadRequestException;
import com.smartlift.model.enums.OrganizationType;
import com.smartlift.security.CustomUserDetailsService;
import com.smartlift.security.JwtTokenProvider;
import com.smartlift.service.UserService;
import org.springframework.context.annotation.Import;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(com.smartlift.config.SecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private UserResponse sampleUserResponse() {
        return UserResponse.builder()
                .id(1L)
                .username("user1")
                .email("user1@test.com")
                .enabled(true)
                .organization(OrganizationSummaryResponse.builder()
                        .id(1L).name("TestOrg").type(OrganizationType.SERVICE).build())
                .roles(Set.of("SERVICE"))
                .build();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllUsers_returns200() throws Exception {
        Page<UserResponse> page = new PageImpl<>(List.of(sampleUserResponse()));
        when(userService.getAllUsers(any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("user1"));
    }

    @Test
    @WithMockUser(roles = "SERVICE")
    void getAllUsers_forbidsNonAdminRole() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGEMENT")
    void getAllUsers_forbidsManagementRole() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllUsers_returns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUserById_returns200() throws Exception {
        when(userService.getUserById(any(), eq(1L))).thenReturn(sampleUserResponse());

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_returns201() throws Exception {
        when(userService.createUser(any(), any(UserRequest.class))).thenReturn(sampleUserResponse());

        UserRequest request = new UserRequest();
        request.setUsername("newuser");
        request.setEmail("new@test.com");
        request.setPassword("password123");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/users/1"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_returns400WhenValidationFails() throws Exception {
        UserRequest request = new UserRequest();
        request.setUsername("");
        request.setEmail("invalid");
        request.setPassword("short");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateUser_returns200() throws Exception {
        when(userService.updateUser(any(), eq(1L), any(UserRequest.class)))
                .thenReturn(sampleUserResponse());

        UserRequest request = new UserRequest();
        request.setUsername("updated");
        request.setEmail("updated@test.com");
        request.setPassword("newpass123");

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteUser_returns204() throws Exception {
        doNothing().when(userService).deleteUser(any(), eq(1L));

        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteUser_returns400WhenDeletingSelf() throws Exception {
        doThrow(new BadRequestException("Cannot delete yourself"))
                .when(userService).deleteUser(any(), eq(1L));

        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isBadRequest());
    }
}
