
package com.securevault.secure_vault.service;

import com.securevault.secure_vault.dto.AuthResponse;
import com.securevault.secure_vault.dto.LoginRequest;
import com.securevault.secure_vault.dto.RegisterRequest;
import com.securevault.secure_vault.exception.AccountLockedException;
import com.securevault.secure_vault.exception.InvalidCredentialsException;
import com.securevault.secure_vault.exception.UserAlreadyExistsException;
import com.securevault.secure_vault.model.Role;
import com.securevault.secure_vault.model.User;
import com.securevault.secure_vault.repository.UserRepository;
import com.securevault.secure_vault.security.CustomUserDetailsService;
import com.securevault.secure_vault.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j                                              
public class AuthService {

     private static final int MAX_FAILED_ATTEMPTS = 5;                 // (2)

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Transactional                                                    // (3)
    public void register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            log.warn("Registration attempt with existing username: {}", request.username());
            throw new UserAlreadyExistsException("Username already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("Email already registered");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password())); // (4)
        user.setEmail(request.email());
        user.setRole(Role.CLIENT);                                    // (5)

        userRepository.save(user);
        log.info("New user registered: {}", request.username());      // (6)
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password")); // (7)

        if (user.isAccountLocked()) {
            log.warn("Login attempt on locked account: {}", request.username());
            throw new AccountLockedException("Account is locked due to multiple failed login attempts");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {  // (8)
            handleFailedLogin(user);
            throw new InvalidCredentialsException("Invalid username or password");
        }

        resetFailedAttempts(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String token = jwtService.generateToken(userDetails);

        log.info("Successful login: {}", request.username());
        return new AuthResponse(token, user.getUsername(), user.getRole().name());
    }

    private void handleFailedLogin(User user) {
        user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
        if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
            user.setAccountLocked(true);
            log.warn("Account locked after {} failed attempts: {}", MAX_FAILED_ATTEMPTS, user.getUsername());
        }
        userRepository.save(user);
    }

    private void resetFailedAttempts(User user) {
        if (user.getFailedLoginAttempts() > 0) {
            user.setFailedLoginAttempts(0);
            userRepository.save(user);
        }
    }
}
