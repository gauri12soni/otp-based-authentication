package com.gauri.otpBasedAuthentication.service;

import com.gauri.otpBasedAuthentication.dto.*;
import com.gauri.otpBasedAuthentication.entity.RefreshToken;
import com.gauri.otpBasedAuthentication.entity.Session;
import com.gauri.otpBasedAuthentication.entity.User;
import com.gauri.otpBasedAuthentication.repository.RefreshTokenRepository;
import com.gauri.otpBasedAuthentication.repository.SessionRepository;
import com.gauri.otpBasedAuthentication.repository.UserRepository;
import com.gauri.otpBasedAuthentication.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OtpService otpService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.access.expiration}")
    private long accessExp;

    @Value("${jwt.refresh.expiration}")
    private long refreshExp;

    public AuthResponse sendOtp(SendOtpRequest request, HttpServletRequest httpRequest) {
        String phone = request.getPhone();
        boolean userExists = userRepository.existsByPhone(phone);

        try {
            String otp = otpService.generateAndSendOtp(phone, !userExists);

            return AuthResponse.builder()
                    .success(true)
                    .statusCode(200)
                    .message(userExists ? "OTP sent for login" : "OTP sent for registration")
                    .data(AuthResponse.AuthData.builder()
                            .expiresIn_otp(600)
                            .userExists(userExists)
                            .build())
                    .build();
        } catch (Exception e) {
            log.error("Failed to send OTP: {}", e.getMessage());
            throw new RuntimeException("Failed to send OTP: " + e.getMessage());
        }
    }

    @Transactional
    public AuthResponse verifyOtp(VerifyOtpRequest request, HttpServletRequest httpRequest) {
        String phone = request.getPhone();
        String otpCode = request.getOtpCode();

        // Verify OTP
        if (!otpService.verifyOtp(phone, otpCode)) {
            throw new RuntimeException("Invalid, expired, or already used OTP");
        }

        // Get or create user
        User user = userRepository.findByPhone(phone)
                .orElseGet(() -> createNewUser(phone));

        // Update last login
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // Enforce one session per user - delete existing session
        Session existingSession = sessionRepository.findByUser(user).orElse(null);
        if (existingSession != null) {
            // Delete associated refresh tokens
            refreshTokenRepository.deleteBySession(existingSession);
            // Delete the session
            sessionRepository.delete(existingSession);
        }

        // Create new session
        Session session = createSession(user, httpRequest);
        sessionRepository.save(session);

        // Generate tokens
        String accessToken = jwtUtil.generateAccessToken(user.getPhone(), session.getId().toString(), accessExp);
        String refreshTokenValue = generateRefreshToken(session);

        // Save refresh token
        saveRefreshToken(refreshTokenValue, session);

        // Build response
        UserResponse userResponse = mapToUserResponse(user);

        return AuthResponse.builder()
                .success(true)
                .statusCode(200)
                .message(user.getCreatedAt() == null ? "Registration successful" : "Login successful")
                .data(AuthResponse.AuthData.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshTokenValue)
                        .expiresIn(accessExp)
                        .tokenType("Bearer")
                        .user(userResponse)
                        .build())
                .build();
    }

    public AuthResponse resendOtp(SendOtpRequest request, HttpServletRequest httpRequest) {
        String phone = request.getPhone();
        boolean userExists = userRepository.existsByPhone(phone);

        try {
            String otp = otpService.resendOtp(phone);

            return AuthResponse.builder()
                    .success(true)
                    .statusCode(200)
                    .message("New OTP sent")
                    .data(AuthResponse.AuthData.builder()
                            .expiresIn_otp(600)
                            .userExists(userExists)
                            .build())
                    .build();
        } catch (Exception e) {
            log.error("Failed to resend OTP: {}", e.getMessage());
            throw new RuntimeException("Failed to resend OTP: " + e.getMessage());
        }
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request, HttpServletRequest httpRequest) {
        String refreshTokenValue = request.getRefreshToken();

        // Hash the token
        String tokenHash = hashToken(refreshTokenValue);

        // Find token in DB
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        // Check if revoked
        if (refreshToken.isRevoked()) {
            throw new RuntimeException("Token has been revoked");
        }

        // Check expiry
        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new RuntimeException("Token has expired");
        }

        // Get session
        Session session = refreshToken.getSession();

        // Validate IP and User-Agent
        if (!session.getIpAddress().equals(getClientIp(httpRequest))) {
            throw new RuntimeException("IP address mismatch");
        }

        if (!session.getUserAgent().equals(httpRequest.getHeader("User-Agent"))) {
            throw new RuntimeException("User-Agent mismatch");
        }

        // Revoke old token
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        // Generate new tokens
        String newAccessToken = jwtUtil.generateAccessToken(
                session.getUser().getPhone(),
                session.getId().toString(),
                accessExp
        );

        String newRefreshTokenValue = generateRefreshToken(session);

        // Save new refresh token
        saveRefreshToken(newRefreshTokenValue, session);

        return AuthResponse.builder()
                .success(true)
                .statusCode(200)
                .message("Token refreshed successfully")
                .data(AuthResponse.AuthData.builder()
                        .accessToken(newAccessToken)
                        .refreshToken(newRefreshTokenValue)
                        .expiresIn(accessExp)
                        .tokenType("Bearer")
                        .build())
                .build();
    }


    @Transactional
    public void logout(String sessionId) {
        try {
            UUID sessionUuid = UUID.fromString(sessionId);
            Session session = sessionRepository.findById(sessionUuid)
                    .orElseThrow(() -> new RuntimeException("Session not found"));

            // Delete associated refresh tokens
            refreshTokenRepository.deleteBySession(session);
            // Delete session
            sessionRepository.delete(session);

            log.info("User logged out successfully. Session ID: {}", sessionId);
        } catch (Exception e) {
            log.error("Error during logout: {}", e.getMessage());
            throw new RuntimeException("Logout failed: " + e.getMessage());
        }
    }

    private User createNewUser(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setRole("USER");
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    private Session createSession(User user, HttpServletRequest request) {
        Session session = new Session();
        session.setUser(user);
        session.setIpAddress(getClientIp(request));
        session.setUserAgent(request.getHeader("User-Agent"));
        session.setCreatedAt(Instant.now());
        session.setExpiresAt(Instant.now().plusSeconds(accessExp));
        session.setLastActiveAt(Instant.now());
        return session;
    }

    private String generateRefreshToken(Session session) {
        return session.getId().toString() + "." + UUID.randomUUID().toString();
    }

    private void saveRefreshToken(String refreshTokenValue, Session session) {
        String tokenHash = hashToken(refreshTokenValue);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setTokenHash(tokenHash);
        refreshToken.setSession(session);
        refreshToken.setRevoked(false);
        refreshToken.setCreatedAt(Instant.now());
        refreshToken.setExpiresAt(Instant.now().plusSeconds(refreshExp));

        refreshTokenRepository.save(refreshToken);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .phone(user.getPhone())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}