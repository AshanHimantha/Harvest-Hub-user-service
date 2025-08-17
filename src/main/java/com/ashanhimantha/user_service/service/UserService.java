package com.ashanhimantha.user_service.service;


import com.ashanhimantha.user_service.dto.response.CognitoUserResponse;

import java.util.List;

public interface UserService {


    CognitoUserResponse getCognitoUserProfile(String userId);

    List<CognitoUserResponse> getAllCognitoUsers(int page, int limit);

    List<CognitoUserResponse> searchUsersByEmail(String email);
}

