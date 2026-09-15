package com.securevault.secure_vault.service;

import com.securevault.securevault.dto.AuthResponse;
import com.securevault.securevault.dto.LoginRequest;
import com.securevault.securevault.dto.RegisterRequest;
import com.securevault.securevault.exception.AccountLockedException;
import com.securevault.securevault.exception.InvalidCredentialsException;
import com.securevault.securevault.exception.UserAlreadyExistsException;
import com.securevault.securevault.model.Role;
import com.securevault.securevault.model.User;
import com.securevault.securevault.repository.UserRepository;
import com.securevault.securevault.security.CustomUserDetailsService;
import com.securevault.securevault.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)   
class AuthServiceTest {

      @Mock private UserRepository userRepository; 
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private CustomUserDetailsService userDetailsService;

    @InjectMocks   
    private AuthService authService;

    private User existingUser;

    @BeforeEach 
    void setUp() {
        existingUser = new User();
        existingUser.setId(1L);
        existingUser.setUsername("jdoe");
        existingUser.setPasswordHash("$2a$10$hashedPasswordExample");
        existingUser.setEmail("jdoe@mail.com");
        existingUser.setRole(Role.CLIENT);
        existingUser.setFailedLoginAttempts(0);
        existingUser.setAccountLocked(false);
    }

    // ---------- register() ----------

    @Test
    void register_shouldSaveUser_whenUsernameAndEmailAreAvailable() {
        // Arrange
        RegisterRequest request = new RegisterRequest("newuser", "Password123", "new@mail.com");
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("hashedValue");

        // Act
        authService.register(request);

        // Assert
        verify(userRepository).save(argThat(user ->  
                user.getUsername().equals("newuser") &&
                user.getPasswordHash().equals("hashedValue") &&
                user.getRole() == Role.CLIENT 
        ));
    }

    @Test
    void register_shouldThrow_whenUsernameAlreadyExists() {
        RegisterRequest request = new RegisterRequest("jdoe", "Password123", "other@mail.com");
        when(userRepository.existsByUsername("jdoe")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, 
                () -> authService.register(request));

        verify(userRepository, never()).save(any());  
    }

    // ---------- login() ----------

    @Test
    void login_shouldReturnToken_whenCredentialsAreValid() {
        LoginRequest request = new LoginRequest("jdoe", "correctPassword");
        UserDetails userDetails = mock(UserDetails.class);

        when(userRepository.findByUsername("jdoe")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("correctPassword", existingUser.getPasswordHash())).thenReturn(true);
        when(userDetailsService.loadUserByUsername("jdoe")).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("fake.jwt.token");

        AuthResponse response = authService.login(request);

        assertEquals("fake.jwt.token", response.token());
        assertEquals("jdoe", response.username());
        assertEquals(0, existingUser.getFailedLoginAttempts());
    }

    @Test
    void login_shouldThrowInvalidCredentials_whenUserDoesNotExist() {
        LoginRequest request = new LoginRequest("ghost", "anyPassword");
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(request));
    }

    @Test
    void login_shouldThrowAccountLocked_whenAccountIsLocked() {
        existingUser.setAccountLocked(true);
        LoginRequest request = new LoginRequest("jdoe", "anyPassword");
        when(userRepository.findByUsername("jdoe")).thenReturn(Optional.of(existingUser));

        assertThrows(AccountLockedException.class,
                () -> authService.login(request));

        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void login_shouldIncrementFailedAttempts_whenPasswordIsWrong() {
        LoginRequest request = new LoginRequest("jdoe", "wrongPassword");
        when(userRepository.findByUsername("jdoe")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("wrongPassword", existingUser.getPasswordHash())).thenReturn(false);

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(request));

        assertEquals(1, existingUser.getFailedLoginAttempts());    
        assertFalse(existingUser.isAccountLocked());
    }

    @Test
    void login_shouldLockAccount_afterFiveFailedAttempts() {
        existingUser.setFailedLoginAttempts(4);                    
        LoginRequest request = new LoginRequest("jdoe", "wrongPassword");
        when(userRepository.findByUsername("jdoe")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("wrongPassword", existingUser.getPasswordHash())).thenReturn(false);

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(request));

        assertEquals(5, existingUser.getFailedLoginAttempts());
        assertTrue(existingUser.isAccountLocked());                    
    }
}
