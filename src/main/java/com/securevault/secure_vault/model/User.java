package com.securevault.secure_vault.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity                                    
@Table(name = "users")                     
@Getter @Setter                           
@NoArgsConstructor @AllArgsConstructor

public class User {
    
     @Id                                     // (5)
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // (6)
    private Long id;

    @Column(unique = true, nullable = false) // (7)
    @NotBlank(message = "Username is required")          // (8)
    @Size(min = 4, max = 30)
    private String username;

    @Column(nullable = false)
    @NotBlank(message = "Password is required")
    private String passwordHash;            // (9)

    @Email(message = "Email must be valid")
    @Column(unique = true, nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)             // (10)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private int failedLoginAttempts = 0;     // (11)

    @Column(nullable = false)
    private boolean accountLocked = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now(); // (12)

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true) // (13)
    private List<Account> accounts;
    
    

}
