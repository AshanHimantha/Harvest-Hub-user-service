package com.ashanhimantha.user_service.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateUserStatusRequest {

    @NotNull(message = "The 'enabled' field is required.")
    private Boolean enabled; // true to enable, false to disable
}