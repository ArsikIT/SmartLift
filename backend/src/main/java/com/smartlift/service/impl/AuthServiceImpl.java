package com.smartlift.service.impl;

import com.smartlift.dto.request.LoginRequest;
import com.smartlift.dto.response.AuthResponse;
import com.smartlift.dto.response.UserResponse;
import com.smartlift.exception.ResourceNotFoundException;
import com.smartlift.mapper.SmartLiftMapper;
import com.smartlift.model.User;
import com.smartlift.repository.UserRepository;
import com.smartlift.security.JwtTokenProvider;
import com.smartlift.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        String token = jwtTokenProvider.generateToken(authentication);
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.getUsername()));
        UserResponse userResponse = SmartLiftMapper.toUserResponse(user);

        return AuthResponse.builder()
                .userId(user.getId())
                .token(token)
                .username(user.getUsername())
                .roles(userResponse.getRoles())
                .organization(userResponse.getOrganization())
                .build();
    }
}
