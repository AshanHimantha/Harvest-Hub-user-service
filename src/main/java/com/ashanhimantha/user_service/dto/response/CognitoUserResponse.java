package com.ashanhimantha.user_service.dto.response;

import lombok.Data;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserType;

import java.util.List;
import java.util.Map;

@Data
public class CognitoUserResponse {
    private String id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private boolean emailVerified;
    private String status;
    private String createdDate;
    private String lastModifiedDate;
    private List<String> userGroups;


    public static CognitoUserResponse fromCognitoAttributes(String userId, Map<String, String> attributes) {
        return fromCognitoAttributes(userId, attributes, null);
    }

    public static CognitoUserResponse fromCognitoAttributes(String userId, Map<String, String> attributes, List<String> userGroups) {
        CognitoUserResponse user = new CognitoUserResponse();
        user.setId(userId);
        user.setEmail(attributes.get("email"));
        user.setFirstName(attributes.get("given_name"));
        user.setLastName(attributes.get("family_name"));
        user.setPhone(attributes.get("phone_number"));
        user.setEmailVerified("true".equals(attributes.get("email_verified")));
        user.setUserGroups(userGroups);
        user.setCreatedDate(attributes.get("created_date"));
        user.setLastModifiedDate(attributes.get("last_modified_date"));
        user.setStatus(attributes.get("status"));

        return user;
    }

    public static CognitoUserResponse fromUserType(UserType userType) {
        return fromUserType(userType, null);
    }

    public static CognitoUserResponse fromUserType(UserType userType, List<String> userGroups) {
        CognitoUserResponse user = new CognitoUserResponse();
        user.setUsername(userType.username());
        user.setStatus(userType.userStatusAsString());
        user.setCreatedDate(userType.userCreateDate().toString());
        user.setLastModifiedDate(userType.userLastModifiedDate().toString());

        // Map attributes
        Map<String, String> attributeMap = userType.attributes().stream()
                .collect(java.util.stream.Collectors.toMap(
                        AttributeType::name,
                        AttributeType::value
                ));

        user.setId(attributeMap.get("sub"));
        user.setEmail(attributeMap.get("email"));
        user.setFirstName(attributeMap.get("given_name"));
        user.setLastName(attributeMap.get("family_name"));
        user.setPhone(attributeMap.get("phone_number"));
        user.setEmailVerified("true".equals(attributeMap.get("email_verified")));
        user.setUserGroups(userGroups);


        return user;
    }
}

