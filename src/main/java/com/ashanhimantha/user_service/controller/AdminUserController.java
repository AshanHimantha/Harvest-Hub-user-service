package com.ashanhimantha.user_service.controller;

import com.ashanhimantha.user_service.dto.response.ApiResponse;
import com.ashanhimantha.user_service.dto.response.CognitoUserResponse;
import com.ashanhimantha.user_service.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
@CrossOrigin(origins = "*")
@PreAuthorize("hasAuthority('SuperAdmins')")
public class AdminUserController {

    private final UserService userService;

    @Autowired
    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Get all users with pagination (Admin only)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CognitoUserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            List<CognitoUserResponse> users = userService.getAllCognitoUsers(page, limit);
            return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve users: " + e.getMessage()));
        }
    }

    /**
     * Get user by ID (Admin only)
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<CognitoUserResponse>> getUserById(@PathVariable String userId) {
        try {
            CognitoUserResponse user = userService.getCognitoUserProfile(userId);
            return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", user));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Search users by email (Admin only)
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<CognitoUserResponse>>> searchUsersByEmail(@RequestParam String email) {
        try {
            List<CognitoUserResponse> users = userService.searchUsersByEmail(email);
            return ResponseEntity.ok(ApiResponse.success("Search completed successfully", users));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Search failed: " + e.getMessage()));
        }
    }
}
