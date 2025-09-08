package com.ashanhimantha.user_service.controller;

import com.ashanhimantha.user_service.dto.request.CreateAdminUserRequest;
import com.ashanhimantha.user_service.dto.request.UpdateUserRoleRequest;
import com.ashanhimantha.user_service.dto.request.UpdateUserStatusRequest;
import com.ashanhimantha.user_service.dto.response.ApiResponse;
import com.ashanhimantha.user_service.dto.response.CognitoUserResponse;
import com.ashanhimantha.user_service.service.CognitoUserService;
import com.ashanhimantha.user_service.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/users/admin")
@CrossOrigin(origins = "*")
@PreAuthorize("hasAuthority('SuperAdmins')")
public class AdminUserController extends AbstractController {

    private final UserService userService;

    @Autowired
    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Get all users with pagination (Admin only)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<CognitoUserService.PaginatedUserResponse>> getAllUsers(
            @RequestParam(defaultValue = "20") @Min(1) @Max(60) int limit,
            @RequestParam(required = false) String nextToken) {

        CognitoUserService.PaginatedUserResponse paginatedResponse = userService.getAllCognitoUsers(limit, nextToken);
        return success("Users retrieved successfully", paginatedResponse);
    }

    /**
     * Get user by ID (Admin only)
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<CognitoUserResponse>> getUserById(@PathVariable String userId) {
        try {
            CognitoUserResponse user = userService.getCognitoUserProfile(userId);
            return success("User retrieved successfully", user);
        } catch (Exception e) {
            return error("User not found", HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Search users by email (Admin only)
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<CognitoUserResponse>>> searchUsersByEmail(@RequestParam String email) {
        List<CognitoUserResponse> users = userService.searchCognitoUsersByEmail(email);
        return success("Search completed successfully", users);
    }

    /**
     * Create a new administrative user (Supplier or DataSteward)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CognitoUserResponse>> createAdminUser(@Valid @RequestBody CreateAdminUserRequest request) {
        try {
            CognitoUserResponse newUser = userService.createCognitoAdminUser(request);
            return created("User created successfully", newUser);
        } catch (RuntimeException e) {
            return error(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/{userId}/role")
    public ResponseEntity<ApiResponse<Void>> updateUserRoles(@PathVariable String userId, @Valid @RequestBody UpdateUserRoleRequest request) {
        try {
            // Convert the list of enums to a list of strings
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
}
