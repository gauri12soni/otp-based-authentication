package com.gauri.otpBasedAuthentication.controller;

import com.gauri.otpBasedAuthentication.dto.AuthResponse;
import com.gauri.otpBasedAuthentication.dto.ErrorResponse;
import com.gauri.otpBasedAuthentication.dto.UpdateProfileRequest;
import com.gauri.otpBasedAuthentication.dto.UserResponse;
import com.gauri.otpBasedAuthentication.entity.User;
import com.gauri.otpBasedAuthentication.repository.UserRepository;
import com.gauri.otpBasedAuthentication.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Authentication authentication){
        try{
            log.debug("getProfile called with authentication: {}", authentication);

            if (authentication == null) {
                log.error("Authentication is null");
                throw new RuntimeException("Authentication required");
            }

            String phone = authentication.getName();
            log.info("Getting profile for phone: {}", phone);

            User user = userRepository.findByPhone(phone)
                    .orElseThrow(() -> {
                        log.error("User not found with phone: {}", phone);
                        return new RuntimeException("User not found");
                    });

            log.debug("User found with ID: {}", user.getId());

            UserResponse userResponse = userService.getProfile(user.getId());

            return ResponseEntity.ok(
                    AuthResponse.builder()
                            .success(true)
                            .statusCode(200)
                            .message("Profile retrieved successfully")
                            .data(AuthResponse.AuthData.builder()
                                    .user(userResponse)
                                    .build())
                            .build()
            );
        }
        catch (Exception e){
            log.error("Error getting profile: {}", e.getMessage(), e);
            return ResponseEntity.status(401).body(
                    ErrorResponse.builder()
                            .success(false)
                            .statusCode(401)
                            .error("UNAUTHORIZED")
                            .message(e.getMessage())
                            .build()
            );
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication) {
        try {
            log.debug("updateProfile called with authentication: {}", authentication);

            if (authentication == null) {
                log.error("Authentication is null");
                throw new RuntimeException("Authentication required");
            }

            String phone = authentication.getName();
            log.info("Updating profile for phone: {}", phone);

            User user = userRepository.findByPhone(phone)
                    .orElseThrow(() -> {
                        log.error("User not found with phone: {}", phone);
                        return new RuntimeException("User not found");
                    });

            UserResponse userResponse = userService.updateProfile(user.getId(), request);

            return ResponseEntity.ok(
                    AuthResponse.builder()
                            .success(true)
                            .statusCode(200)
                            .message("Profile updated successfully")
                            .data(AuthResponse.AuthData.builder()
                                    .user(userResponse)
                                    .build())
                            .build()
            );
        } catch (Exception e) {
            log.error("Error updating profile: {}", e.getMessage(), e);
            if (e.getMessage().contains("email")) {
                return ResponseEntity.badRequest().body(
                        ErrorResponse.builder()
                                .success(false)
                                .statusCode(400)
                                .error("INVALID_EMAIL")
                                .message("Invalid email format")
                                .build()
                );
            }
            return ResponseEntity.status(401).body(
                    ErrorResponse.builder()
                            .success(false)
                            .statusCode(401)
                            .error("UNAUTHORIZED")
                            .message(e.getMessage())
                            .build()
            );
        }
    }
}