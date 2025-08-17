package com.ashanhimantha.user_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CognitoUserService {

    private final String userPoolId;
    private final CognitoIdentityProviderClient cognitoClient;

    public CognitoUserService(
            @Value("${aws.cognito.userPoolId}") String userPoolId,
            @Value("${aws.region}") String awsRegion) {
        this.userPoolId = userPoolId;
        this.cognitoClient = CognitoIdentityProviderClient.builder()
                .region(Region.of(awsRegion))
                .build();
    }

    public Map<String, String> getUserAttributes(String userId) {
        try {
            AdminGetUserRequest request = AdminGetUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(userId)
                    .build();

            AdminGetUserResponse response = cognitoClient.adminGetUser(request);

            // Get user attributes
            Map<String, String> attributes = response.userAttributes().stream()
                    .collect(Collectors.toMap(
                            AttributeType::name,
                            AttributeType::value
                    ));

            // Add metadata fields that are not in user attributes
            attributes.put("status", response.userStatusAsString());
            if (response.userCreateDate() != null) {
                attributes.put("created_date", response.userCreateDate().toString());
            }
            if (response.userLastModifiedDate() != null) {
                attributes.put("last_modified_date", response.userLastModifiedDate().toString());
            }

            return attributes;
        } catch (CognitoIdentityProviderException e) {
            throw new RuntimeException("Failed to fetch user from Cognito: " + e.getMessage(), e);
        }
    }

    public List<UserType> listUsers(int page, int limit) {
        try {
            ListUsersRequest.Builder requestBuilder = ListUsersRequest.builder()
                    .userPoolId(userPoolId)
                    .limit(limit);

            // Handle pagination - Cognito uses paginationToken instead of page number
            if (page > 0) {
                // Note: This is a simplified pagination. In real implementation,
                // you'd need to store/manage pagination tokens properly
                requestBuilder.paginationToken(String.valueOf(page));
            }

            ListUsersResponse response = cognitoClient.listUsers(requestBuilder.build());
            return response.users();
        } catch (CognitoIdentityProviderException e) {
            throw new RuntimeException("Failed to list users from Cognito: " + e.getMessage(), e);
        }
    }

    public List<UserType> searchUsersByEmail(String email) {
        try {
            ListUsersRequest request = ListUsersRequest.builder()
                    .userPoolId(userPoolId)
                    .filter("email = \"" + email + "\"")
                    .build();

            ListUsersResponse response = cognitoClient.listUsers(request);
            return response.users();
        } catch (CognitoIdentityProviderException e) {
            throw new RuntimeException("Failed to search users from Cognito: " + e.getMessage(), e);
        }
    }

    public List<String> getUserGroups(String username) {
        try {
            AdminListGroupsForUserRequest request = AdminListGroupsForUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(username)
                    .build();

            AdminListGroupsForUserResponse response = cognitoClient.adminListGroupsForUser(request);

            return response.groups().stream()
                    .map(GroupType::groupName)
                    .collect(Collectors.toList());
        } catch (CognitoIdentityProviderException e) {
            throw new RuntimeException("Failed to fetch user groups from Cognito: " + e.getMessage(), e);
        }
    }
}
