package com.ashanhimantha.user_service.service;


import com.ashanhimantha.user_service.dto.request.CreateAdminUserRequest;
import com.ashanhimantha.user_service.dto.response.CognitoUserResponse;

import java.util.List;

public interface UserService {


    CognitoUserResponse getCognitoUserProfile(String userId);

    List<CognitoUserResponse> getAllCognitoUsers(int page, int limit);

    List<CognitoUserResponse> searchUsersByEmail(String email);

    CognitoUserResponse createCognitoAdminUser(CreateAdminUserRequest request);

    void syncCognitoUserRoles(String username, List<String> newRoles);

    void updateCognitoUserStatus(String username, boolean enable);
}

