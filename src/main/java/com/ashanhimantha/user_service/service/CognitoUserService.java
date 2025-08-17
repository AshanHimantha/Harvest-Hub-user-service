package com.ashanhimantha.user_service.service;

import com.ashanhimantha.user_service.dto.request.CreateAdminUserRequest;
import com.ashanhimantha.user_service.dto.response.CognitoUserResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UnsupportedOperationException;

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


    public CognitoUserResponse createAdminUser(CreateAdminUserRequest request) {
        try {
            // 1. Prepare user attributes
            List<AttributeType> userAttributes = List.of(
                    AttributeType.builder().name("email").value(request.getEmail()).build(),
                    AttributeType.builder().name("given_name").value(request.getFirstName()).build(),
                    AttributeType.builder().name("family_name").value(request.getLastName()).build(),
                    AttributeType.builder().name("name").value(request.getFirstName() + " " + request.getLastName()).build(),
                    AttributeType.builder().name("email_verified").value("true").build()
            );

            // 2. Create the user in Cognito
            AdminCreateUserRequest createUserRequest = AdminCreateUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(request.getEmail())
                    .userAttributes(userAttributes)
                    .desiredDeliveryMediums(DeliveryMediumType.EMAIL)
                    .build();

            AdminCreateUserResponse createUserResponse = cognitoClient.adminCreateUser(createUserRequest);
            UserType createdUser = createUserResponse.user();
            String username = createdUser.username();
            String userId = createdUser.attributes().stream()
                    .filter(attr -> "sub".equals(attr.name()))
                    .findFirst()
                    .map(AttributeType::value)
                    .orElse(null);

            // 3. Add the user to the specified group
            String groupName = request.getRole().name();
            AdminAddUserToGroupRequest addUserToGroupRequest = AdminAddUserToGroupRequest.builder()
                    .userPoolId(userPoolId)
                    .username(username)
                    .groupName(groupName)
                    .build();

            cognitoClient.adminAddUserToGroup(addUserToGroupRequest);

            // 4. Manually construct the response DTO. This avoids the problematic second API call.
            CognitoUserResponse response = new CognitoUserResponse();
            response.setId(userId);
            response.setUsername(username);
            response.setEmail(request.getEmail());
            response.setFirstName(request.getFirstName());
            response.setLastName(request.getLastName());
            response.setEmailVerified(true);
            response.setUserGroups(List.of(groupName));
            response.setStatus(createdUser.userStatusAsString());
            response.setCreatedDate(String.valueOf(createdUser.userCreateDate()));
            response.setLastModifiedDate(String.valueOf(createdUser.userLastModifiedDate()));

            return response;

        } catch (UsernameExistsException e) {
            throw new RuntimeException("A user with this email already exists.");
        } catch (InvalidParameterException e) {
            throw new RuntimeException("Invalid parameter provided: ".concat(e.awsErrorDetails().errorMessage()));
        } catch (CognitoIdentityProviderException e) {
            throw new RuntimeException("Failed to create user in Cognito: ".concat(e.awsErrorDetails().errorMessage()), e);
        }
    }


    public void syncUserRoles(String username, List<String> newRoles) {
        try {
            // 1. Get the list of groups the user is currently a member of
            AdminListGroupsForUserRequest listGroupsRequest = AdminListGroupsForUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(username)
                    .build();
            AdminListGroupsForUserResponse listGroupsResponse = cognitoClient.adminListGroupsForUser(listGroupsRequest);

            List<String> currentRoles = listGroupsResponse.groups().stream()
                    .map(GroupType::groupName)
                    .toList();

            // 2. Determine which roles to add
            List<String> rolesToAdd = newRoles.stream()
                    .filter(newRole -> !currentRoles.contains(newRole))
                    .toList();

            // 3. Determine which roles to remove
            List<String> rolesToRemove = currentRoles.stream()
                    .filter(currentRole -> !newRoles.contains(currentRole))
                    .toList();

            // 4. Perform the add operations
            for (String role : rolesToAdd) {
                AdminAddUserToGroupRequest addRequest = AdminAddUserToGroupRequest.builder()
                        .userPoolId(userPoolId)
                        .username(username)
                        .groupName(role)
                        .build();
                cognitoClient.adminAddUserToGroup(addRequest);
            }

            // 5. Perform the remove operations
            for (String role : rolesToRemove) {
                AdminRemoveUserFromGroupRequest removeRequest = AdminRemoveUserFromGroupRequest.builder()
                        .userPoolId(userPoolId)
                        .username(username)
                        .groupName(role)
                        .build();
                cognitoClient.adminRemoveUserFromGroup(removeRequest);
            }

        } catch (UserNotFoundException e) {
            throw new RuntimeException("User not found in Cognito.");
        } catch (CognitoIdentityProviderException e) {
            throw new RuntimeException("Failed to update user roles in Cognito: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    public void updateUserStatus(String username, boolean enable) {
        try {
            if (enable) {
                // Logic to ENABLE the user
                AdminEnableUserRequest request = AdminEnableUserRequest.builder()
                        .userPoolId(userPoolId)
                        .username(username)
                        .build();
                cognitoClient.adminEnableUser(request);
            } else {
                // Logic to DISABLE the user, including the safety check

                // --- SAFETY CHECK START ---
                AdminListGroupsForUserRequest listGroupsRequest = AdminListGroupsForUserRequest.builder()
                        .userPoolId(userPoolId)
                        .username(username)
                        .build();
                AdminListGroupsForUserResponse listGroupsResponse = cognitoClient.adminListGroupsForUser(listGroupsRequest);

                boolean isSuperAdmin = listGroupsResponse.groups().stream()
                        .anyMatch(group -> group.groupName().equalsIgnoreCase("SuperAdmins"));

                if (isSuperAdmin) {
                    throw new java.lang.UnsupportedOperationException("Security Violation: Cannot disable a SuperAdmin user.");
                }
                // --- SAFETY CHECK END ---

                AdminDisableUserRequest request = AdminDisableUserRequest.builder()
                        .userPoolId(userPoolId)
                        .username(username)
                        .build();
                cognitoClient.adminDisableUser(request);
            }
        } catch (UserNotFoundException e) {
            throw new RuntimeException("User not found in Cognito with username: " + username);
        } catch (UnsupportedOperationException e) {
            throw e; // Re-throw our specific exception
        } catch (CognitoIdentityProviderException e) {
            throw new RuntimeException("Failed to update user status in Cognito: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

}
