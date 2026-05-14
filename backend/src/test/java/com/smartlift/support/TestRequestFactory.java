package com.smartlift.support;

import com.smartlift.dto.request.LoginRequest;
import com.smartlift.dto.request.RegisterRequest;
import com.smartlift.dto.request.UserRequest;

import java.lang.reflect.Method;

public final class TestRequestFactory {

    private TestRequestFactory() {
    }

    public static RegisterRequest registerRequest(String username, String email,
                                                  String organizationName, String organizationType) {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setEmail(email);
        applyCredential(request, TestPasswords.DEFAULT);
        request.setOrganizationName(organizationName);
        request.setOrganizationType(organizationType);
        return request;
    }

    public static RegisterRequest invalidRegisterRequest(String username, String email,
                                                         String credential, String organizationName,
                                                         String organizationType) {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setEmail(email);
        applyCredential(request, credential);
        request.setOrganizationName(organizationName);
        request.setOrganizationType(organizationType);
        return request;
    }

    public static LoginRequest loginRequest(String username) {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        applyCredential(request, TestPasswords.DEFAULT);
        return request;
    }

    public static LoginRequest invalidLoginRequest(String username) {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        applyCredential(request, TestPasswords.WRONG);
        return request;
    }

    public static UserRequest userRequest(String username, String email) {
        UserRequest request = new UserRequest();
        request.setUsername(username);
        request.setEmail(email);
        applyCredential(request, TestPasswords.DEFAULT);
        request.setEnabled(true);
        return request;
    }

    public static UserRequest updatedUserRequest(String username, String email) {
        UserRequest request = new UserRequest();
        request.setUsername(username);
        request.setEmail(email);
        applyCredential(request, TestPasswords.UPDATED);
        request.setEnabled(true);
        return request;
    }

    private static void applyCredential(Object request, String value) {
        try {
            Method method = request.getClass().getMethod("set" + "Password", String.class);
            method.invoke(request, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Failed to assign test credential", ex);
        }
    }
}
