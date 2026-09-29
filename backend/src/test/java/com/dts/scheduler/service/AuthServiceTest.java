package com.dts.scheduler.service;

import com.dts.scheduler.dto.auth.AuthResponse;
import com.dts.scheduler.dto.auth.RegisterRequest;
import com.dts.scheduler.entity.Role;
import com.dts.scheduler.entity.User;
import com.dts.scheduler.exception.ConflictException;
import com.dts.scheduler.repository.UserRepository;
import com.dts.scheduler.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequest.builder()
                .name("Alex Engineer")
                .email("alex@example.com")
                .password("supersecret123")
                .build();
    }

    @Test
    @DisplayName("Should register new user and return JWT tokens")
    void testRegisterSuccess() {
        when(userRepository.existsByEmail("alex@example.com")).thenReturn(false);
        when(passwordEncoder.encode("supersecret123")).thenReturn("hashedPassword");

        User savedUser = User.builder()
                .id(1L)
                .name("Alex Engineer")
                .email("alex@example.com")
                .passwordHash("hashedPassword")
                .role(Role.ROLE_USER)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateAccessToken(1L, "alex@example.com", Role.ROLE_USER)).thenReturn("mockAccessToken");
        when(jwtService.generateRefreshToken(1L, "alex@example.com", Role.ROLE_USER)).thenReturn("mockRefreshToken");

        AuthResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("mockAccessToken", response.getAccessToken());
        assertEquals("mockRefreshToken", response.getRefreshToken());
        assertEquals("alex@example.com", response.getUser().getEmail());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should reject registration if email already exists")
    void testRegisterDuplicateEmailThrowsConflict() {
        when(userRepository.existsByEmail("alex@example.com")).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () ->
                authService.register(registerRequest));

        assertTrue(ex.getMessage().contains("already registered"));
        verify(userRepository, never()).save(any(User.class));
    }
}
