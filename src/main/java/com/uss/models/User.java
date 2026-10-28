package com.uss.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
        @UniqueConstraint(name = "uk_users_username", columnNames = "username")
})
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String email;
    private String username;
    private String password;
    private String role="ROLE_USER";
    private String profilePictureKey;

//    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
//    private UrlMapping urlMapping;
}
