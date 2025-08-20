package com.ashanhimantha.user_service.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Entity
@Table(name = "addresses")
@Data
@SQLDelete(sql = "UPDATE addresses SET active = false WHERE id = ?") // Override DELETE command
@Where(clause = "active = true")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "active", nullable = false, columnDefinition = "boolean default true")
    private boolean active = true;

    @Column(name = "user_id", nullable = false)
    private String userId;

    private String street;
    private String city;
    private String state;
    private String postalCode;
    private String country;
}