package com.ashanhimantha.user_service.controller;


import com.ashanhimantha.user_service.dto.response.CognitoUserResponse;
import com.ashanhimantha.user_service.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

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

            String firstName = jwt.getClaimAsString("given_name");
            String lastName = jwt.getClaimAsString("family_name");
            fallbackResponse.setFirstName(firstName);
            fallbackResponse.setLastName(lastName);

            // Try to get phone
            fallbackResponse.setPhone(jwt.getClaimAsString("phone_number"));

            // Set email verified status
            Object emailVerified = jwt.getClaim("email_verified");
            if (emailVerified != null) {
                fallbackResponse.setEmailVerified(Boolean.parseBoolean(emailVerified.toString()));
            }

            // Set metadata fields from JWT if available
            fallbackResponse.setStatus("CONFIRMED"); // Default status for authenticated users

            // Try to get creation date from JWT (iat claim - issued at)
            Long iat = jwt.getClaimAsInstant("iat") != null ? jwt.getClaimAsInstant("iat").getEpochSecond() : null;
            if (iat != null) {
                fallbackResponse.setCreatedDate(java.time.Instant.ofEpochSecond(iat).toString());
                fallbackResponse.setLastModifiedDate(java.time.Instant.ofEpochSecond(iat).toString());
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

}