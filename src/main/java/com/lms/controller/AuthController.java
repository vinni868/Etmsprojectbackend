package com.lms.controller;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.lms.dto.LoginRequest;
import com.lms.dto.LoginResponse;
import com.lms.dto.RegisterRequest;
import com.lms.entity.User;
import com.lms.enums.Status;
import com.lms.repository.UserRepository;
import com.lms.service.UserService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;

    @Autowired
    private UserService userService;

    AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // =========================
    // REGISTER
    // =========================
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {

        User user = userService.register(request);

        if (user == null) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Registration failed"));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Registration successful",
                "userId", String.valueOf(user.getId())
        ));
    }

    // =========================
    // LOGIN (SESSION CREATION)
    // =========================
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest request,
            HttpSession session) {

        try {

            User user = userService.login(
                    request.getEmail(),
                    request.getPassword()
            );

            session.setAttribute("USER_ID", user.getId());
            session.setAttribute("ROLE", user.getRole().getRoleName());

            LoginResponse response = new LoginResponse(
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getRole().getRoleName()
            );

            return ResponseEntity.ok(response);

        } catch (RuntimeException ex) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", ex.getMessage()));
        }
    }

    // =========================
    // LOGOUT
    // =========================
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {

        session.invalidate();

        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> payload) {

        String email = payload.get("email");

        Optional<User> optionalUser = userRepository.findByEmail(email);

        if (optionalUser.isEmpty()) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("message", "Trainer not found"));
        }

        User user = optionalUser.get();

        if (!"TRAINER".equals(user.getRole().getRoleName())) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("message", "Trainer not found"));
        }

        System.out.println("Trainer requested password reset: " + email);

        return ResponseEntity.ok(Map.of(
                "message", "Admin notified to reset password."
        ));
    }
}
