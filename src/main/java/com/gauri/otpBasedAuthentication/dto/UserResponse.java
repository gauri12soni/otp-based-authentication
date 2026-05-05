package com.gauri.otpBasedAuthentication.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class UserResponse {
    private UUID id;
    private String phone;
    private String fullName;
    private String email;
    private String role;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastLoginAt;
}