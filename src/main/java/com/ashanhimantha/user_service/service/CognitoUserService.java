package com.ashanhimantha.user_service.service;

import com.ashanhimantha.user_service.dto.request.CreateAdminUserRequest;
import com.ashanhimantha.user_service.dto.response.CognitoUserResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import java.lang.UnsupportedOperationException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CognitoUserService {

    private final CognitoIdentityProviderClient cognitoClient;
    private final String userPoolId;

    /**
     * A simple DTO/Record for returning paginated results from Cognito.
     */
    public record PaginatedUserResponse(List<CognitoUserResponse> users, String nextToken) {}

    public CognitoUserService(@Value("${aws.cognito.userPoolId}") String userPoolId,
                              @Value("${aws.region}") String awsRegion) {
        this.userPoolId = userPoolId;
        this.cognitoClient = CognitoIdentityProviderClient.builder()
                .region(Region.of(awsRegion))
                .build();
    }

    /**
     * Fetches a single user's full profile from Cognito by their username.
     */
    public CognitoUserResponse getUserProfileByUsername(String username) {
        try {
            AdminGetUserRequest request = AdminGetUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(username)
                    .build();
            AdminGetUserResponse response = cognitoClient.adminGetUser(request);
            List<String> groups = getGroupsForUser(username);
            return mapToCognitoUserResponse(response, groups);
        } catch (UserNotFoundException e) {
            throw new RuntimeException("User not found: " + username);
        } catch (CognitoIdentityProviderException e) {
            throw new RuntimeException("Failed to fetch user from Cognito", e);
        }
    }

    /**
     * Fetches a paginated list of all users from Cognito.
     */
    public PaginatedUserResponse listUsers(int limit, String paginationToken) {
        try {
            ListUsersRequest.Builder requestBuilder = ListUsersRequest.builder()
                    .userPoolId(userPoolId)
                    .limit(limit);

            if (paginationToken != null && !paginationToken.isEmpty()) {
                requestBuilder.paginationToken(paginationToken);
            }

            ListUsersResponse response = cognitoClient.listUsers(requestBuilder.build());
            List<CognitoUserResponse> userList = response.users().stream()
                    .map(userType -> {
                        List<String> groups = getGroupsForUser(userType.username());
                        return mapToCognitoUserResponse(userType, groups);
                    })
                    .collect(Collectors.toList());

            return new PaginatedUserResponse(userList, response.paginationToken());
        } catch (CognitoIdentityProviderException e) {
            throw new RuntimeException("Failed to list users from Cognito: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a new administrative user (Supplier, DataSteward) and sends them an invitation email.
     */
    public CognitoUserResponse createAdminUser(CreateAdminUserRequest request) {
        try {
            AdminCreateUserRequest createUserRequest = AdminCreateUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(request.getEmail())
                    .userAttributes(
                            AttributeType.builder().name("email").value(request.getEmail()).build(),
                            AttributeType.builder().name("given_name").value(request.getFirstName()).build(),
                            AttributeType.builder().name("family_name").value(request.getLastName()).build(),
                            AttributeType.builder().name("name").value(request.getFirstName() + " " + request.getLastName()).build(),
                            AttributeType.builder().name("email_verified").value("true").build()
                    )
                    .desiredDeliveryMediums(DeliveryMediumType.EMAIL)
                    .build();

            AdminCreateUserResponse createUserResponse = cognitoClient.adminCreateUser(createUserRequest);
            UserType createdUser = createUserResponse.user();
            String groupName = request.getRole().name();

            addUserToGroup(createdUser.username(), groupName);

            return getUserProfileByUsername(createdUser.username());

        } catch (UsernameExistsException e) {
            throw new RuntimeException("A user with this email already exists.");
        } catch (CognitoIdentityProviderException e) {
            throw new RuntimeException("Failed to create user in Cognito: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    /**
     * Syncs a user's roles/groups with the provided list. Prevents modification of SuperAdmins.
     */
    public void syncUserRoles(String username, List<String> newRoles) {
        try {
            List<String> currentRoles = getGroupsForUser(username);
            if (currentRoles.contains("SuperAdmins")) {
                throw new UnsupportedOperationException("Security Violation: Cannot modify roles for a SuperAdmin user.");
            }

            List<String> rolesToAdd = newRoles.stream().filter(r -> !currentRoles.contains(r)).toList();
            List<String> rolesToRemove = currentRoles.stream().filter(r -> !newRoles.contains(r)).toList();

            for (String role : rolesToAdd) addUserToGroup(username, role);
            for (String role : rolesToRemove) removeUserFromGroup(username, role);

        } catch (UserNotFoundException e) {
            throw new RuntimeException("User not found: " + username);
        } catch (CognitoIdentityProviderException e) {
            throw new RuntimeException("Failed to update user roles: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    /**
     * Updates a user's status (enabled/disabled). Prevents disabling SuperAdmins.
     */
    public void updateUserStatus(String username, boolean enable) {
        try {
            if (enable) {
                AdminEnableUserRequest request = AdminEnableUserRequest.builder().userPoolId(userPoolId).username(username).build();
                cognitoClient.adminEnableUser(request);
            } else {
                if (getGroupsForUser(username).contains("SuperAdmins")) {
                    throw new UnsupportedOperationException("Security Violation: Cannot disable a SuperAdmin user.");
                }
                AdminDisableUserRequest request = AdminDisableUserRequest.builder().userPoolId(userPoolId).username(username).build();
                cognitoClient.adminDisableUser(request);
            }
        } catch (UserNotFoundException e) {
            throw new RuntimeException("User not found: " + username);
        } catch (UnsupportedOperationException e) {
            throw e;
        } catch (CognitoIdentityProviderException e) {
            throw new RuntimeException("Failed to update user status: " + e.awsErrorDetails().errorMessage(), e);
        }
    }


    // --- PRIVATE HELPER METHODS ---

    private List<String> getGroupsForUser(String username) {
        try {
            AdminListGroupsForUserRequest request = AdminListGroupsForUserRequest.builder().userPoolId(userPoolId).username(username).build();
            AdminListGroupsForUserResponse response = cognitoClient.adminListGroupsForUser(request);
            return response.groups().stream().map(GroupType::groupName).collect(Collectors.toList());
        } catch (Exception e) {
            return List.of();
        }
    }

    private void addUserToGroup(String username, String groupName) {
        AdminAddUserToGroupRequest request = AdminAddUserToGroupRequest.builder().userPoolId(userPoolId).username(username).groupName(groupName).build();
        cognitoClient.adminAddUserToGroup(request);
    }

    private void removeUserFromGroup(String username, String groupName) {
        AdminRemoveUserFromGroupRequest request = AdminRemoveUserFromGroupRequest.builder().userPoolId(userPoolId).username(username).groupName(groupName).build();
        cognitoClient.adminRemoveUserFromGroup(request);
    }

    private CognitoUserResponse mapToCognitoUserResponse(AdminGetUserResponse cognitoUser, List<String> groups) {
        Map<String, String> attributes = cognitoUser.userAttributes().stream().collect(Collectors.toMap(AttributeType::name, AttributeType::value));
        return buildCognitoUserResponse(attributes, cognitoUser.username(), cognitoUser.userStatusAsString(), cognitoUser.userCreateDate(), cognitoUser.userLastModifiedDate(), groups);
    }

    private CognitoUserResponse mapToCognitoUserResponse(UserType cognitoUser, List<String> groups) {
        Map<String, String> attributes = cognitoUser.attributes().stream().collect(Collectors.toMap(AttributeType::name, AttributeType::value));
        return buildCognitoUserResponse(attributes, cognitoUser.username(), cognitoUser.userStatusAsString(), cognitoUser.userCreateDate(), cognitoUser.userLastModifiedDate(), groups);
    }

    private CognitoUserResponse buildCognitoUserResponse(Map<String, String> attributes, String username, String status, java.time.Instant createDate, java.time.Instant modifiedDate, List<String> groups) {
        CognitoUserResponse response = new CognitoUserResponse();
        response.setId(getAttributeValue(attributes, "sub").orElse(null));
        response.setUsername(username);
        response.setEmail(getAttributeValue(attributes, "email").orElse(null));
        response.setFirstName(getAttributeValue(attributes, "given_name").orElse(null));
        response.setLastName(getAttributeValue(attributes, "family_name").orElse(null));
        response.setPhone(getAttributeValue(attributes, "phone_number").orElse(null));
        response.setEmailVerified(getAttributeValue(attributes, "email_verified").map(Boolean::parseBoolean).orElse(false));
        response.setStatus(status);
        response.setCreatedDate(String.valueOf(createDate));
        response.setLastModifiedDate(String.valueOf(modifiedDate));
        response.setUserGroups(groups);
        return response;
    }

    private Optional<String> getAttributeValue(Map<String, String> attributes, String key) {
        return Optional.ofNullable(attributes.get(key));
    }

    public List<CognitoUserResponse> searchUsersByEmail(String email) {
        try {
            // The filter syntax for Cognito requires the attribute name and a comparison.
            // Using "starts with" (^) is generally more flexible than exact match (=).
            String filter = "email ^= \"" + email + "\"";

            ListUsersRequest request = ListUsersRequest.builder()
                    .userPoolId(userPoolId)
                    .filter(filter)
                    .build();

            ListUsersResponse response = cognitoClient.listUsers(request);

            // Map the results to our DTO
            return response.users().stream()
                    .map(userType -> {
                        List<String> groups = getGroupsForUser(userType.username());
                        return mapToCognitoUserResponse(userType, groups);
                    })
                    .collect(Collectors.toList());

        } catch (CognitoIdentityProviderException e) {
            throw new RuntimeException("Failed to search users from Cognito: " + e.getMessage(), e);
        }
    }

}