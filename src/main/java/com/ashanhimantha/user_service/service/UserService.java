package com.ashanhimantha.user_service.service;


import com.ashanhimantha.user_service.dto.request.AddressRequest;
import com.ashanhimantha.user_service.dto.request.CreateAdminUserRequest;
import com.ashanhimantha.user_service.dto.response.CognitoUserResponse;
import com.ashanhimantha.user_service.entity.Address;

import java.util.List;
import java.util.Optional;

public interface UserService {


    CognitoUserResponse getCognitoUserProfile(String userId);

    List<CognitoUserResponse> getAllCognitoUsers(int page, int limit);

    List<CognitoUserResponse> searchUsersByEmail(String email);

    CognitoUserResponse createCognitoAdminUser(CreateAdminUserRequest request);

    void syncCognitoUserRoles(String username, List<String> newRoles);

    void updateCognitoUserStatus(String username, boolean enable);

    Address addAddressForUser(String userId, AddressRequest addressRequest);
    List<Address> getAddressesForUser(String userId);

    Optional<Address> updateUserAddress(String userId, Long addressId, AddressRequest addressRequest);
}

