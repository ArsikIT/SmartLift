package com.smartlift.service;

import com.smartlift.dto.request.UserRequest;
import com.smartlift.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    Page<UserResponse> getAllUsers(String currentUsername, Pageable pageable);

    UserResponse getUserById(String currentUsername, Long id);

    UserResponse createUser(String currentUsername, UserRequest request);

    UserResponse updateUser(String currentUsername, Long id, UserRequest request);

    void deleteUser(String currentUsername, Long id);
}
