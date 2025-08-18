package com.ashanhimantha.user_service.service;

import com.ashanhimantha.user_service.dto.request.AddressRequest;
import com.ashanhimantha.user_service.dto.request.CreateAdminUserRequest;
import com.ashanhimantha.user_service.dto.response.CognitoUserResponse;
import com.ashanhimantha.user_service.entity.Address;
import com.ashanhimantha.user_service.service.CognitoUserService.PaginatedUserResponse;

import java.util.List;
import java.util.Optional;

public interface UserService {

    // === Cognito User Management ===
    CognitoUserResponse getCognitoUserProfile(String username);
    PaginatedUserResponse getAllCognitoUsers(int limit, String nextToken);
    CognitoUserResponse createCognitoAdminUser(CreateAdminUserRequest request);
    void syncCognitoUserRoles(String username, List<String> newRoles);
    void updateCognitoUserStatus(String username, boolean enable);
    List<CognitoUserResponse> searchCognitoUsersByEmail(String email);
    // === Local Address Management ===
    Address addAddressForUser(String userId, AddressRequest addressRequest);
    List<Address> getAddressesForUser(String userId);
    Optional<Address> updateUserAddress(String userId, Long addressId, AddressRequest addressRequest);
    boolean deleteUserAddress(String userId, Long addressId);
}