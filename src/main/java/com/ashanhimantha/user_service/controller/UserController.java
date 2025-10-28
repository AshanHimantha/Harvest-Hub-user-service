package com.ashanhimantha.user_service.controller;

import com.ashanhimantha.user_service.dto.request.AddressRequest;
import com.ashanhimantha.user_service.dto.request.CreateAdminUserRequest;
import com.ashanhimantha.user_service.dto.request.UpdateUserRoleRequest;
import com.ashanhimantha.user_service.dto.request.UpdateUserStatusRequest;
import com.ashanhimantha.user_service.dto.response.ApiResponse;
import com.ashanhimantha.user_service.dto.response.CognitoUserResponse;
import com.ashanhimantha.user_service.entity.Address;
import com.ashanhimantha.user_service.service.CognitoUserService;
import com.ashanhimantha.user_service.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/users")
@CrossOrigin(origins = "*")
public class UserController extends AbstractController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    // ==================== Public/User Endpoints ====================

    /**
     * Get current user profile from Cognito
     */
    @GetMapping("/currentUser")
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
            String email = jwt.getClaimAsString("email");
            if (email == null) {
                email = jwt.getClaimAsString("username");
            }
            fallbackResponse.setEmail(email);
            fallbackResponse.setFirstName(jwt.getClaimAsString("given_name"));
            fallbackResponse.setLastName(jwt.getClaimAsString("family_name"));
            fallbackResponse.setPhone(jwt.getClaimAsString("phone_number"));
            Object emailVerified = jwt.getClaim("email_verified");
            if (emailVerified != null) {
                fallbackResponse.setEmailVerified(Boolean.parseBoolean(emailVerified.toString()));
            }
            fallbackResponse.setStatus("CONFIRMED");
            java.time.Instant iatInstant = jwt.getClaimAsInstant("iat");
            if (iatInstant != null) {
                fallbackResponse.setCreatedDate(iatInstant.toString());
                fallbackResponse.setLastModifiedDate(iatInstant.toString());
            }
            Object groupsClaim = jwt.getClaim("cognito:groups");
            if (groupsClaim instanceof List<?>) {
                @SuppressWarnings("unchecked")
                List<String> userGroups = (List<String>) groupsClaim;
                fallbackResponse.setUserGroups(userGroups);
            } else {
                fallbackResponse.setUserGroups(List.of());
            }
            return ResponseEntity.ok(fallbackResponse);
        }
    }

    @PostMapping("/currentUser/addresses")
    public ResponseEntity<ApiResponse<Address>> addMyAddress(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AddressRequest addressRequest) {
        String userId = jwt.getSubject();
        try {
            Address savedAddress = userService.addAddressForUser(userId, addressRequest);
            return created("Address added successfully", savedAddress);
        } catch (Exception e) {
            return error("Failed to add address: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/currentUser/addresses")
    public ResponseEntity<ApiResponse<List<Address>>> getMyAddresses(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        try {
            List<Address> addresses = userService.getAddressesForUser(userId);
            return success("Addresses retrieved successfully", addresses);
        } catch (Exception e) {
            return error("Failed to retrieve addresses: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/currentUser/addresses/{addressId}")
    public ResponseEntity<ApiResponse<Address>> updateMyAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long addressId,
            @Valid @RequestBody AddressRequest addressRequest) {
        String userId = jwt.getSubject();
        try {
            Optional<Address> updatedAddressOptional = userService.updateUserAddress(userId, addressId, addressRequest);
            return updatedAddressOptional.map(address -> success("Address updated successfully", address))
                    .orElseGet(() -> error("Address not found or you do not have permission to update it.", HttpStatus.NOT_FOUND));
        } catch (Exception e) {
            return error("Failed to update address: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/currentUser/addresses/{addressId}")
    public ResponseEntity<ApiResponse<Void>> deleteMyAddress(@AuthenticationPrincipal Jwt jwt, @PathVariable Long addressId) {
        String userId = jwt.getSubject();
        boolean deleted = userService.deleteUserAddress(userId, addressId);
        if (deleted) {
            return success("Address deleted successfully", null);
        } else {
            return error("Address not found or you do not have permission to delete it.", HttpStatus.NOT_FOUND);
        }
    }

    // ==================== Admin Endpoints ====================

    @GetMapping
    @PreAuthorize("hasAuthority('SuperAdmins')")
    public ResponseEntity<ApiResponse<CognitoUserService.PaginatedUserResponse>> getAllUsers(
            @RequestParam(defaultValue = "20") @Min(1) @Max(60) int limit,
            @RequestParam(required = false) String nextToken) {
        CognitoUserService.PaginatedUserResponse paginatedResponse = userService.getAllCognitoUsers(limit, nextToken);
        return success("Users retrieved successfully", paginatedResponse);
    }

    /**
     * Search users by multiple criteria (Admin only).
     * This endpoint is called with a URL like:
     * /api/v1/users/search?role=Suppliers
     * /api/v1/users/search?email=test@example.com
     */
    @GetMapping("/search")
    @PreAuthorize("hasAuthority('SuperAdmins')")
    public ResponseEntity<ApiResponse<List<CognitoUserResponse>>> searchUsers(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role) {

        // This logging is very helpful for debugging to see exactly what the server received.
        System.out.println(
                String.format("--- SEARCHING USERS --- Received params: email=[%s], firstName=[%s], lastName=[%s], username=[%s], status=[%s], role=[%s]",
                        email, firstName, lastName, username, status, role)
        );

        // If only the 'email' parameter is provided, use a more efficient Cognito query.
        if (email != null && firstName == null && lastName == null && username == null && status == null && role == null) {
            List<CognitoUserResponse> users = userService.searchCognitoUsersByEmail(email);
            return success("Search completed successfully", users);
        }

        // For any other combination of parameters, use the general-purpose search method
        // which fetches all users and filters them in the application.
        List<CognitoUserResponse> users = userService.searchCognitoUsers(email, firstName, lastName, username, status, role);
        return success("Search completed successfully", users);
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAuthority('SuperAdmins')")
    public ResponseEntity<ApiResponse<CognitoUserResponse>> getUserById(@PathVariable String userId) {
        try {
            CognitoUserResponse user = userService.getCognitoUserProfile(userId);
            return success("User retrieved successfully", user);
        } catch (Exception e) {
            return error("User not found", HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SuperAdmins')")
    public ResponseEntity<ApiResponse<CognitoUserResponse>> createAdminUser(@Valid @RequestBody CreateAdminUserRequest request) {
        try {
            CognitoUserResponse newUser = userService.createCognitoAdminUser(request);
            return created("User created successfully", newUser);
        } catch (RuntimeException e) {
            return error(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/{userId}/role")
    @PreAuthorize("hasAuthority('SuperAdmins')")
    public ResponseEntity<ApiResponse<Void>> updateUserRoles(@PathVariable String userId, @Valid @RequestBody UpdateUserRoleRequest request) {
        try {
            List<String> roleNames = request.getRoles().stream()
                    .map(Enum::name)
                    .collect(Collectors.toList());
            userService.syncCognitoUserRoles(userId, roleNames);
            return success("User roles updated successfully", null);
        } catch (RuntimeException e) {
            return error(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/{userId}/status")
    @PreAuthorize("hasAuthority('SuperAdmins')")
    public ResponseEntity<ApiResponse<Void>> updateUserStatus(@PathVariable String userId, @Valid @RequestBody UpdateUserStatusRequest request) {
        try {
            userService.updateCognitoUserStatus(userId, request.getEnabled());
            String status = request.getEnabled() ? "enabled" : "disabled";
            return success("User status successfully updated to " + status, null);
        } catch (UnsupportedOperationException e) {
            return error(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (RuntimeException e) {
            return error(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/employees")
    @PreAuthorize("hasAuthority('SuperAdmins')")
    public ResponseEntity<ApiResponse<List<CognitoUserResponse>>> getEmployeeUsers() {
        try {
            List<CognitoUserResponse> employees = userService.getEmployeeUsers();
            return success("Employee users retrieved successfully", employees);
        } catch (Exception e) {
            return error("Failed to retrieve employee users: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}