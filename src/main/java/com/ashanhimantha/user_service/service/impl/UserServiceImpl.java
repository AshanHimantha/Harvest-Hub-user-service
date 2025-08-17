package com.ashanhimantha.user_service.service.impl;

import com.ashanhimantha.user_service.dto.request.UserProfileRequest;
import com.ashanhimantha.user_service.dto.response.CognitoUserResponse;
import com.ashanhimantha.user_service.entity.User;
import com.ashanhimantha.user_service.repository.UserRepository;
import com.ashanhimantha.user_service.service.CognitoUserService;
import com.ashanhimantha.user_service.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final CognitoUserService cognitoUserService;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, CognitoUserService cognitoUserService) {
        this.userRepository = userRepository;
        this.cognitoUserService = cognitoUserService;
    }

    @Override
    public Optional<User> getUserProfile(String userId) {
        return userRepository.findById(userId);
    }

    @Override
    public User createOrUpdateUserProfile(String userId, String email, UserProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElse(new User());

        user.setId(userId);
        user.setEmail(email);
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());

        return userRepository.save(user);
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