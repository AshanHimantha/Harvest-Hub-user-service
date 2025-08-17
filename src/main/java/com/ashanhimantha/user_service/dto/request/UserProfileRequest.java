package com.ashanhimantha.user_service.dto.request;

import lombok.Data;

@Data // Lombok annotation for getters, setters, toString, etc.
public class UserProfileRequest {
    private String firstName;
    private String lastName;

}