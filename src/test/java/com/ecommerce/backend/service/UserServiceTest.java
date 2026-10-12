package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.AuthRequest;
import com.ecommerce.backend.dto.AuthResponse;
import com.ecommerce.backend.dto.RegisterRequest;
import com.ecommerce.backend.model.User;
import com.ecommerce.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private PasswordEncoder passwordEncoder;
    private UserService userService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        userService = new UserService(userRepository, passwordEncoder);
    }

    @Test
    void registerAlwaysCreatesUserAndStoresEncodedPassword() {
        when(userRepository.existsByEmail("shopper@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = userService.register(
                new RegisterRequest("Shopper", "shopper@example.com", "user-password", "ADMIN"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("USER", userCaptor.getValue().getRole());
        assertTrue(passwordEncoder.matches("user-password", userCaptor.getValue().getPassword()));
        assertFalse(userCaptor.getValue().getPassword().equals("user-password"));
        assertEquals("USER", response.getRole());
    }

    @Test
    void loginMatchesEncodedPassword() {
        User user = new User("Shopper", "shopper@example.com", passwordEncoder.encode("user-password"), "USER");
        when(userRepository.findByEmail("shopper@example.com")).thenReturn(Optional.of(user));

        AuthResponse response = userService.login(new AuthRequest("shopper@example.com", "user-password"));

        assertEquals("Login successful", response.getMessage());
        assertEquals("USER", response.getRole());
    }

    @Test
    void loginUpgradesLegacyPlaintextPasswordAfterSuccessfulLogin() {
        User user = new User("Shopper", "shopper@example.com", "legacy-password", "USER");
        when(userRepository.findByEmail("shopper@example.com")).thenReturn(Optional.of(user));

        AuthResponse response = userService.login(new AuthRequest("shopper@example.com", "legacy-password"));

        assertEquals("Login successful", response.getMessage());
        assertTrue(passwordEncoder.matches("legacy-password", user.getPassword()));
        verify(userRepository).save(user);
    }

    @Test
    void loginRejectsIncorrectPassword() {
        User user = new User("Shopper", "shopper@example.com", passwordEncoder.encode("user-password"), "USER");
        when(userRepository.findByEmail("shopper@example.com")).thenReturn(Optional.of(user));

        AuthResponse response = userService.login(new AuthRequest("shopper@example.com", "wrong-password"));

        assertEquals("Invalid email or password", response.getMessage());
    }
}
