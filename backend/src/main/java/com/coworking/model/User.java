package com.coworking.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    private String firstName;
    private String lastName;
    private String phone;

    // "member", "manager", "admin"
    @Column(nullable = false)
    private String type = "member";

    // "ACTIVE", "INACTIVE", "PENDING"
    @Column(nullable = false)
    private String status = "PENDING";

    private String profilePicture;

    // Manager fields
    private String companyName;
    private String companyAddress;
    private String companyRegNumber;
    private String companyTaxId;
}
