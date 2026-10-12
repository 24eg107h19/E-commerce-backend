package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.AuthRequest;
import com.ecommerce.backend.dto.AuthResponse;
import com.ecommerce.backend.dto.RegisterRequest;
import com.ecommerce.backend.service.JwtTokenService;
import com.ecommerce.backend.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtTokenService jwtTokenService;

    public AuthController(UserService userService, JwtTokenService jwtTokenService) {
        this.userService = userService;
        this.jwtTokenService = jwtTokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        AuthResponse response = userService.register(request);
        if (response.getId() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        addAccessToken(response);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        AuthResponse response = userService.login(request);
        if (response.getId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        addAccessToken(response);
        return ResponseEntity.ok(response);
    }

    private void addAccessToken(AuthResponse response) {
        response.setAccessToken(jwtTokenService.createToken(response.getId(), response.getRole()));
        response.setTokenType("Bearer");
    }
}
