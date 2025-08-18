package com.ashanhimantha.user_service.controller;


import com.ashanhimantha.user_service.dto.request.AddressRequest;
import com.ashanhimantha.user_service.dto.response.ApiResponse;
import com.ashanhimantha.user_service.dto.response.CognitoUserResponse;
import com.ashanhimantha.user_service.entity.Address;
import com.ashanhimantha.user_service.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

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
     * Base users endpoint - returns 404 with specific error message
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Object>> getUsers() {
        ApiResponse<Object> response = ApiResponse.error("Internal server error: No static resource api/v1/users.");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
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


    @PostMapping("/me/addresses")
    public ResponseEntity<ApiResponse<Address>> addMyAddress(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AddressRequest addressRequest) {
        String userId = jwt.getSubject(); // Get the user ID from the token

        try {
            Address savedAddress = userService.addAddressForUser(userId, addressRequest);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Address added successfully", savedAddress));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to add address: " + e.getMessage()));
        }
    }

    /**
     * Get all addresses for the current user.
     */
    @GetMapping("/me/addresses")
    public ResponseEntity<ApiResponse<List<Address>>> getMyAddresses(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject(); // Get the user ID from the token

        try {
            List<Address> addresses = userService.getAddressesForUser(userId);
            return ResponseEntity.ok(ApiResponse.success("Addresses retrieved successfully", addresses));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve addresses: " + e.getMessage()));
        }
    }

    @PutMapping("/me/addresses/{addressId}")
    public ResponseEntity<ApiResponse<Address>> updateMyAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long addressId,
            @Valid @RequestBody AddressRequest addressRequest) {

        String userId = jwt.getSubject(); // Get the user ID from the token

        try {
            Optional<Address> updatedAddressOptional = userService.updateUserAddress(userId, addressId, addressRequest);

            // Check if the address was found and updated
            if (updatedAddressOptional.isPresent()) {
                return ResponseEntity.ok(ApiResponse.success("Address updated successfully", updatedAddressOptional.get()));
            } else {
                // If the Optional is empty, it means the address was not found or the user doesn't own it.
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Address not found or you do not have permission to update it."));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to update address: " + e.getMessage()));
        }
    }

    @DeleteMapping("/me/addresses/{addressId}")
    public ResponseEntity<ApiResponse<?>> deleteMyAddress(@AuthenticationPrincipal Jwt jwt, @PathVariable Long addressId) {
        String userId = jwt.getSubject();
        boolean deleted = userService.deleteUserAddress(userId, addressId);

        if (deleted) {
            return ResponseEntity.ok(ApiResponse.success("Address deleted successfully", null));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Address not found or you do not have permission to delete it."));
        }
    }

}