package com.smartlift.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {

    private final String token;
    private final String username;
}
