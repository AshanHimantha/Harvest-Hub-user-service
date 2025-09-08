package com.ashanhimantha.user_service.service;

import com.ashanhimantha.user_service.dto.request.AddressRequest;
import com.ashanhimantha.user_service.dto.request.CreateAdminUserRequest;
import com.ashanhimantha.user_service.dto.response.CognitoUserResponse;
import com.ashanhimantha.user_service.entity.Address;
import com.ashanhimantha.user_service.service.CognitoUserService.PaginatedUserResponse;

import java.util.List;
import java.util.Optional;

public abstract class UserService {

    // === Cognito User Management ===
    public abstract CognitoUserResponse getCognitoUserProfile(String userId);
    public abstract PaginatedUserResponse getAllCognitoUsers(int limit, String nextToken);
    public abstract CognitoUserResponse createCognitoAdminUser(CreateAdminUserRequest request);
    public abstract void syncCognitoUserRoles(String userId, List<String> newRoles);
    public abstract void updateCognitoUserStatus(String userId, boolean enable);
    public abstract List<CognitoUserResponse> searchCognitoUsersByEmail(String email);

    // === Local Address Management ===
    public abstract Address addAddressForUser(String userId, AddressRequest addressRequest);
    public abstract List<Address> getAddressesForUser(String userId);
    public abstract Optional<Address> updateUserAddress(String userId, Long addressId, AddressRequest addressRequest);
    public abstract boolean deleteUserAddress(String userId, Long addressId);

    // === Common helper methods that can be shared ===
    protected void validateUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
    }

    protected void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Invalid email format");
        }
    }
}