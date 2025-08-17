package com.ashanhimantha.user_service.service.impl;

import com.ashanhimantha.user_service.dto.response.CognitoUserResponse;
import com.ashanhimantha.user_service.service.CognitoUserService;
import com.ashanhimantha.user_service.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {


    private final CognitoUserService cognitoUserService;

    @Autowired
    public UserServiceImpl(CognitoUserService cognitoUserService) {
        this.cognitoUserService = cognitoUserService;
    }


    @Override
    public CognitoUserResponse getCognitoUserProfile(String userId) {
        Map<String, String> userAttributes = cognitoUserService.getUserAttributes(userId);
        List<String> userGroups = cognitoUserService.getUserGroups(userId);
        return CognitoUserResponse.fromCognitoAttributes(userId, userAttributes, userGroups);
    }

    @Override
    public List<CognitoUserResponse> getAllCognitoUsers(int page, int limit) {
        return cognitoUserService.listUsers(page, limit).stream()
                .map(CognitoUserResponse::fromUserType)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public List<CognitoUserResponse> searchUsersByEmail(String email) {
        return cognitoUserService.searchUsersByEmail(email).stream()
                .map(CognitoUserResponse::fromUserType)
                .collect(java.util.stream.Collectors.toList());
    }
}