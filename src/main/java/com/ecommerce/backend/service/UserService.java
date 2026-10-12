package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.AuthRequest;
import com.ecommerce.backend.dto.AuthResponse;
import com.ecommerce.backend.dto.RegisterRequest;
import com.ecommerce.backend.model.User;
import com.ecommerce.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            return new AuthResponse(null, null, null, null, "Email is already registered");
        }

        User user = new User(
                request.getName(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                "USER"
        );

        User savedUser = userRepository.save(user);
        return new AuthResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole(),
                "Registration successful"
        );
    }

    public AuthResponse login(AuthRequest request) {
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
        if (userOpt.isEmpty()) {
            return new AuthResponse(null, null, null, null, "Invalid email or password");
        }

        User user = userOpt.get();
        String storedPassword = user.getPassword();
        if (isBcryptHash(storedPassword)) {
            if (!passwordEncoder.matches(request.getPassword(), storedPassword)) {
                return new AuthResponse(null, null, null, null, "Invalid email or password");
            }
        } else if (Objects.equals(storedPassword, request.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            userRepository.save(user);
        } else {
            return new AuthResponse(null, null, null, null, "Invalid email or password");
        }

        return new AuthResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                "Login successful"
        );
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    private boolean isBcryptHash(String password) {
        return password != null && (password.startsWith("$2a$")
                || password.startsWith("$2b$")
                || password.startsWith("$2y$"));
    }
}
