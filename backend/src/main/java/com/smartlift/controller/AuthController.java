package com.smartlift.controller;

import com.smartlift.dto.request.LoginRequest;
import com.smartlift.dto.request.RegisterRequest;
import com.smartlift.dto.response.AuthResponse;
import com.smartlift.dto.response.UserResponse;
import com.smartlift.service.AuthService;
import com.smartlift.service.RegistrationService;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final RegistrationService registrationService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse created = registrationService.register(request);
        return ResponseEntity.created(URI.create("/api/users/" + created.getId())).body(created);
    }
}
