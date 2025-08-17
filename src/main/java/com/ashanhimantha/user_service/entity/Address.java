package com.ashanhimantha.user_service.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "addresses")
@Data
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // This is the crucial change.
    // We store the Cognito User ID directly.
    @Column(name = "user_id", nullable = false)
    private String userId;

    private String street;
    private String city;
    private String state;
    private String postalCode;
    private String country;
}