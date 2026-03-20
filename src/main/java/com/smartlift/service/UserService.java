package com.smartlift.service;

import com.smartlift.dto.UserRequest;
import com.smartlift.dto.UserResponse;
import java.util.List;

public interface UserService {

    List<UserResponse> getAllUsers();

    UserResponse getUserById(Long id);

    UserResponse createUser(UserRequest request);

    UserResponse updateUser(Long id, UserRequest request);

    void deleteUser(Long id);
}
