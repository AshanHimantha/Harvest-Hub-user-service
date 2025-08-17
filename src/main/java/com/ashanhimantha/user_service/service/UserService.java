package com.ashanhimantha.user_service.service;


import com.ashanhimantha.user_service.dto.request.UserProfileRequest;
import com.ashanhimantha.user_service.dto.response.CognitoUserResponse;
import com.ashanhimantha.user_service.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserService {
    Optional<User> getUserProfile(String userId);

    User createOrUpdateUserProfile(String userId, String email, UserProfileRequest request);

    CognitoUserResponse getCognitoUserProfile(String userId);

    List<CognitoUserResponse> getAllCognitoUsers(int page, int limit);

    List<CognitoUserResponse> searchUsersByEmail(String email);
}
