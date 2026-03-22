package com.smartlift.service;

import com.smartlift.dto.request.RegisterRequest;
import com.smartlift.dto.response.UserResponse;

public interface RegistrationService {

    UserResponse register(RegisterRequest request);
}
