package com.smartlift.service;

import com.smartlift.dto.request.LoginRequest;
import com.smartlift.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse login(LoginRequest request);
}
