package com.ashanhimantha.user_service.controller;


import com.ashanhimantha.user_service.dto.request.UserProfileRequest;
import com.ashanhimantha.user_service.dto.response.CognitoUserResponse;
import com.ashanhimantha.user_service.entity.User;
import com.ashanhimantha.user_service.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Get current user profile from Cognito
     */
    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();

        try {
            CognitoUserResponse userProfile = userService.getCognitoUserProfile(userId);
            return ResponseEntity.ok(userProfile);
        } catch (Exception e) {
            // Fallback: return info from JWT if Cognito fails
            CognitoUserResponse fallbackResponse = new CognitoUserResponse();
            fallbackResponse.setId(userId);
            fallbackResponse.setUsername(userId);

            // Try to get email from different possible claims
            String email = jwt.getClaimAsString("email");
            if (email == null) {
                email = jwt.getClaimAsString("username");
            }
            fallbackResponse.setEmail(email);

            // Try to get name information
            String firstName = jwt.getClaimAsString("given_name");
            String lastName = jwt.getClaimAsString("family_name");
            fallbackResponse.setFirstName("hi");
            fallbackResponse.setLastName(lastName);


                fallbackResponse.setName(jwt.getClaimAsString("name"));


            // Try to get phone
            fallbackResponse.setPhone(jwt.getClaimAsString("phone_number"));

            // Set email verified status
            Object emailVerified = jwt.getClaim("email_verified");
            if (emailVerified != null) {
                fallbackResponse.setEmailVerified(Boolean.parseBoolean(emailVerified.toString()));
            }

            // Try to get user groups from JWT claims
            Object groupsClaim = jwt.getClaim("cognito:groups");
            if (groupsClaim instanceof List<?>) {
                @SuppressWarnings("unchecked")
                List<String> userGroups = (List<String>) groupsClaim;
                fallbackResponse.setUserGroups(userGroups);
            } else {
                // Set empty list as default if no groups found
                fallbackResponse.setUserGroups(List.of());
            }

            return ResponseEntity.ok(fallbackResponse);
        }
    }

    /**
     * Create or update current user profile in local database
     */
    @PostMapping("/me")
    public ResponseEntity<User> createOrUpdateMyProfile(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UserProfileRequest request) {
        String userId = jwt.getSubject();
        String email = jwt.getClaimAsString("email");

        User savedUser = userService.createOrUpdateUserProfile(userId, email, request);
        return ResponseEntity.ok(savedUser);
    }

    /**
     * Update current user profile in local database
     */
    @PutMapping("/me")
    public ResponseEntity<User> updateMyProfile(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UserProfileRequest request) {
        String userId = jwt.getSubject();
        String email = jwt.getClaimAsString("email");

        User updatedUser = userService.createOrUpdateUserProfile(userId, email, request);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * Get user profile by ID from Cognito (admin endpoint)
     */
    @GetMapping("/{userId}")
    public ResponseEntity<CognitoUserResponse> getUserById(@PathVariable String userId) {
        try {
            CognitoUserResponse userProfile = userService.getCognitoUserProfile(userId);
            return ResponseEntity.ok(userProfile);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get all users from Cognito (admin endpoint)
     */
    @GetMapping
    public ResponseEntity<List<CognitoUserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            List<CognitoUserResponse> users = userService.getAllCognitoUsers(page, limit);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Search users by email from Cognito (admin endpoint)
     */
    @GetMapping("/search")
    public ResponseEntity<List<CognitoUserResponse>> searchUsers(@RequestParam String email) {
        try {
            List<CognitoUserResponse> users = userService.searchUsersByEmail(email);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get current user's local profile
     */
    @GetMapping("/me/local")
    public ResponseEntity<User> getMyLocalProfile(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        return userService.getUserProfile(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get current user info from JWT (no external calls) - for debugging
     */
    @GetMapping("/me/jwt")
    public ResponseEntity<?> getMyJwtInfo(@AuthenticationPrincipal Jwt jwt) {
        // Return all JWT claims for debugging
        return ResponseEntity.ok(jwt.getClaims());
    }
}