package com.ashanhimantha.user_service.service;

import com.ashanhimantha.user_service.dto.request.CreateAdminUserRequest;
import com.ashanhimantha.user_service.dto.response.CognitoUserResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import java.lang.UnsupportedOperationException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CognitoUserService {

    private static final Logger logger = LoggerFactory.getLogger(CognitoUserService.class);

    private final CognitoIdentityProviderClient cognitoClient;
    private final String userPoolId;

    public record PaginatedUserResponse(List<CognitoUserResponse> users, String nextToken) {}

    public CognitoUserService(@Value("${aws.cognito.userPoolId}") String userPoolId,
                              @Value("${aws.region}") String awsRegion) {
        this.userPoolId = userPoolId;
        this.cognitoClient = CognitoIdentityProviderClient.builder()
                .region(Region.of(awsRegion))
                .build();
    }

    // === UNCHANGED METHODS (getUserProfileByUsername, createAdminUser, etc.) ===
    // These methods work on a single user and are fine as they are.

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

    public List<CognitoUserResponse> searchUsersByEmail(String email) {
        try {
            String filter = "email ^= \"" + email + "\"";
            ListUsersRequest request = ListUsersRequest.builder()
                    .userPoolId(userPoolId)
                    .filter(filter)
                    .build();
            ListUsersResponse response = cognitoClient.listUsers(request);
            // This method might still be slow if the email returns many users.
            // For simplicity, we can reuse getGroupsForUser here, as the user count is expected to be small.
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


    // ========================================================================
    // === REWRITTEN AND OPTIMIZED METHODS FOR SEARCHING AND LISTING USERS ====
    // ========================================================================

    /**
     * [OPTIMIZED] Fetches a paginated list of all users from Cognito with their group info.
     */
    public PaginatedUserResponse listUsers(int limit, String paginationToken) {
        try {
            // Step 1: Fetch all group memberships efficiently.
            Map<String, List<String>> userGroupMappings = fetchAllUserGroupMappings();

            // Step 2: Fetch the requested page of users.
            ListUsersRequest request = ListUsersRequest.builder()
                    .userPoolId(userPoolId)
                    .limit(limit)
                    .paginationToken(paginationToken)
                    .build();
            ListUsersResponse response = cognitoClient.listUsers(request);

            // Step 3: Combine user data with the group map.
            List<CognitoUserResponse> userList = response.users().stream()
                    .map(userType -> {
                        List<String> groups = userGroupMappings.getOrDefault(userType.username(), Collections.emptyList());
                        return mapToCognitoUserResponse(userType, groups);
                    })
                    .collect(Collectors.toList());

            return new PaginatedUserResponse(userList, response.paginationToken());
        } catch (CognitoIdentityProviderException e) {
            throw new RuntimeException("Failed to list users from Cognito: " + e.getMessage(), e);
        }
    }

    /**
     * [OPTIMIZED] Search users by multiple criteria using the efficient fetching strategy.
     */
    public List<CognitoUserResponse> searchUsers(String email, String firstName, String lastName, String username, String status, String role) {
        try {
            // Fetch all users with their group information in an efficient manner.
            List<CognitoUserResponse> allUsers = getAllUsersWithGroupInfo();

            // Apply filters in memory (this is fast).
            return allUsers.stream()
                    .filter(user -> {
                        if (email != null && !email.isEmpty() && (user.getEmail() == null || !user.getEmail().toLowerCase().contains(email.toLowerCase()))) return false;
                        if (firstName != null && !firstName.isEmpty() && (user.getFirstName() == null || !user.getFirstName().toLowerCase().contains(firstName.toLowerCase()))) return false;
                        if (lastName != null && !lastName.isEmpty() && (user.getLastName() == null || !user.getLastName().toLowerCase().contains(lastName.toLowerCase()))) return false;
                        if (username != null && !username.isEmpty() && (user.getUsername() == null || !user.getUsername().toLowerCase().contains(username.toLowerCase()))) return false;
                        if (status != null && !status.isEmpty() && (user.getStatus() == null || !user.getStatus().equalsIgnoreCase(status))) return false;
                        if (role != null && !role.isEmpty()) {
                            boolean roleMatch = user.getUserGroups() != null && user.getUserGroups().stream()
                                    .anyMatch(userRole -> userRole.toLowerCase().contains(role.toLowerCase()));
                            if (!roleMatch) return false;
                        }
                        return true;
                    })
                    .collect(Collectors.toList());

        } catch (CognitoIdentityProviderException e) {
            throw new RuntimeException("Failed to search users from Cognito: " + e.getMessage(), e);
        }
    }

    /**
     * [NEW HELPER] Efficiently fetches all users with their group info.
     * This replaces the old, inefficient `getAllUsers` method.
     */
    private List<CognitoUserResponse> getAllUsersWithGroupInfo() {
        // Step 1: Fetch all group memberships efficiently.
        Map<String, List<String>> userGroupMappings = fetchAllUserGroupMappings();

        // Step 2: Fetch all users by paginating through the results.
        List<UserType> allCognitoUsers = new ArrayList<>();
        String paginationToken = null;
        do {
            ListUsersRequest request = ListUsersRequest.builder()
                    .userPoolId(userPoolId)
                    .limit(60) // Max limit
                    .paginationToken(paginationToken)
                    .build();
            ListUsersResponse response = cognitoClient.listUsers(request);
            allCognitoUsers.addAll(response.users());
            paginationToken = response.paginationToken();
        } while (paginationToken != null);

        // Step 3: Combine user data with the group map. This is done in memory and is very fast.
        return allCognitoUsers.stream()
                .map(userType -> {
                    List<String> groups = userGroupMappings.getOrDefault(userType.username(), Collections.emptyList());
                    return mapToCognitoUserResponse(userType, groups);
                })
                .collect(Collectors.toList());
    }

    /**
     * [NEW HELPER] Builds a map of `username -> List<groupName>` for all users.
     * This is the key to the performance improvement.
     */
    private Map<String, List<String>> fetchAllUserGroupMappings() {
        Map<String, List<String>> userToGroupsMap = new HashMap<>();

        // 1. Get all groups in the user pool
        ListGroupsResponse listGroupsResponse = cognitoClient.listGroups(ListGroupsRequest.builder().userPoolId(userPoolId).build());
        List<GroupType> groups = listGroupsResponse.groups();

        // 2. For each group, get all users in it
        for (GroupType group : groups) {
            String groupName = group.groupName();
            String paginationToken = null;
            do {
                ListUsersInGroupRequest usersInGroupRequest = ListUsersInGroupRequest.builder()
                        .userPoolId(userPoolId)
                        .groupName(groupName)
                        .nextToken(paginationToken)
                        .build();
                ListUsersInGroupResponse usersInGroupResponse = cognitoClient.listUsersInGroup(usersInGroupRequest);

                // 3. Add each user to the map
                for (UserType user : usersInGroupResponse.users()) {
                    userToGroupsMap.computeIfAbsent(user.username(), k -> new ArrayList<>()).add(groupName);
                }
                paginationToken = usersInGroupResponse.nextToken();
            } while (paginationToken != null);
        }
        return userToGroupsMap;
    }


    // --- UNCHANGED PRIVATE HELPERS ---

    private List<String> getGroupsForUser(String username) {
        try {
            AdminListGroupsForUserRequest request = AdminListGroupsForUserRequest.builder()
                    .userPoolId(userPoolId).username(username).build();
            AdminListGroupsForUserResponse response = cognitoClient.adminListGroupsForUser(request);
            return response.groups().stream().map(GroupType::groupName).collect(Collectors.toList());
        } catch (CognitoIdentityProviderException e) {
            logger.error("Failed to fetch groups for user {}: {}", username, e.awsErrorDetails().errorMessage());
            throw new RuntimeException("Failed to fetch user groups from Cognito", e);
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
        String status = cognitoUser.enabled() ? "ENABLED" : "DISABLED";
        return buildCognitoUserResponse(attributes, cognitoUser.username(), status, cognitoUser.userCreateDate(), cognitoUser.userLastModifiedDate(), groups);
    }

    private CognitoUserResponse mapToCognitoUserResponse(UserType cognitoUser, List<String> groups) {
        Map<String, String> attributes = cognitoUser.attributes().stream().collect(Collectors.toMap(AttributeType::name, AttributeType::value));
        String status = cognitoUser.enabled() != null && cognitoUser.enabled() ? "ENABLED" : "DISABLED";
        return buildCognitoUserResponse(attributes, cognitoUser.username(), status, cognitoUser.userCreateDate(), cognitoUser.userLastModifiedDate(), groups);
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
        response.setUserGroups(groups != null ? groups : Collections.emptyList());
        return response;
    }

    private Optional<String> getAttributeValue(Map<String, String> attributes, String key) {
        return Optional.ofNullable(attributes.get(key));
    }


    public String getUsernameByUserId(String userId) {
        try {
            // Cognito's filter syntax requires searching by the 'sub' attribute
            String filter = "sub = \"" + userId + "\"";

            ListUsersRequest request = ListUsersRequest.builder()
                    .userPoolId(userPoolId)
                    .filter(filter)
                    .limit(1) // We only expect one result
                    .build();

            ListUsersResponse response = cognitoClient.listUsers(request);

            if (response.users() == null || response.users().isEmpty()) {
                logger.warn("Could not find a user with ID (sub): {}", userId);
                throw new RuntimeException("User not found with ID: " + userId);
            }

            // Return the username of the first (and only) user found
            return response.users().get(0).username();

        } catch (CognitoIdentityProviderException e) {
            logger.error("Failed to find user by ID '{}' from Cognito: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to find user by ID from Cognito: " + e.getMessage(), e);
        }
    }

    // In: CognitoUserService.java

    /**
     * [NEW METHOD] Finds all users that belong to ANY of the specified groups.
     * This is used to fetch specific categories of users, like "employees".
     */
    public List<CognitoUserResponse> findUsersByGroups(List<String> groupNamesToFind) {
        // Step 1: Get all users with their group info efficiently.
        List<CognitoUserResponse> allUsers = getAllUsersWithGroupInfo();

        // Step 2: Filter the list in memory.
        return allUsers.stream()
                .filter(user -> {
                    // Check if the user has any groups assigned at all.
                    if (user.getUserGroups() == null || user.getUserGroups().isEmpty()) {
                        return false;
                    }
                    // Check if the user's groups have any overlap with the groups we're looking for.
                    // !Collections.disjoint returns true if the lists share at least one element.
                    return !Collections.disjoint(user.getUserGroups(), groupNamesToFind);
                })
                .collect(Collectors.toList());
    }
}

