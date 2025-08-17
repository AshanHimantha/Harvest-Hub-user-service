package com.ashanhimantha.user_service.dto.request;

import com.ashanhimantha.user_service.enums.UserRole;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class UpdateUserRoleRequest {

    @NotNull(message = "Roles list cannot be null")
    @NotEmpty(message = "User must have at least one role")
    private List<UserRole> roles; // Changed from a single role to a List


}