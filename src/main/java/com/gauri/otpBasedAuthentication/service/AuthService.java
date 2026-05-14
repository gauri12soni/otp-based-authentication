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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
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

    @Value("${jwt.access.expiration}")
    private long accessExp;

    @Value("${jwt.refresh.expiration}")
    private long refreshExp;

    @Value("${otp.expiration.minutes}")
    private long otpExpiration;

    private final TransactionTemplate transactionTemplate;

    public AuthResponse sendOtp(SendOtpRequest request, HttpServletRequest httpRequest) {
        String phone = request.getPhone();
        boolean userExists = userRepository.existsByPhone(phone);

        try {
            otpService.generateAndSendOtp(phone, !userExists);

            return AuthResponse.builder()
                    .success(true)
                    .statusCode(200)
                    .message(userExists ? "OTP sent for login" : "OTP sent for registration")
                    .data(AuthResponse.AuthData.builder()
                            .expiresIn(otpExpiration * 60) // minutes to seconds
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

        // Invalidate ALL unused OTPs for this phone
        otpService.invalidateUnusedOtps(phone);

        // Get or create user
        boolean isNewUser = !userRepository.existsByPhone(phone);
        User user = userRepository.findByPhone(phone)
                .orElseGet(() -> createNewUser(phone));

        // Update last login
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        // One session per user - delete existing session
        Session existingSession = sessionRepository.findByUser(user).orElse(null);
        if (existingSession != null) {
            refreshTokenRepository.deleteBySession(existingSession);
            sessionRepository.delete(existingSession);
            sessionRepository.flush();
            refreshTokenRepository.flush();
        }

        Session session = createSession(user, httpRequest);
        sessionRepository.save(session);

        String accessToken = jwtUtil.generateAccessToken(user.getId().toString(), session.getId().toString(), accessExp);
        String refreshTokenValue = generateRefreshToken(session);
        saveRefreshToken(refreshTokenValue, session);

        UserResponse userResponse = mapToUserResponse(user);

        return AuthResponse.builder()
                .success(true)
                .statusCode(200)
                .message(isNewUser ? "Registration successful" : "Login successful")
                .data(AuthResponse.AuthData.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshTokenValue)
                        .expiresIn(accessExp / 1000) // ms  to seconds
                        .tokenType("Bearer")
                        .user(userResponse)
                        .build())
                .build();
    }

    public AuthResponse resendOtp(SendOtpRequest request, HttpServletRequest httpRequest) {
        String phone = request.getPhone();
        boolean userExists = userRepository.existsByPhone(phone);

        try {
            otpService.resendOtp(phone);

            return AuthResponse.builder()
                    .success(true)
                    .statusCode(200)
                    .message("New OTP sent")
                    .data(AuthResponse.AuthData.builder()
                            .expiresIn(otpExpiration * 60)
                            .userExists(userExists)
                            .build())
                    .build();
        } catch (Exception e) {
            log.error("Failed to resend OTP: {}", e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    // NEW REFRESH TOKEN LOGIC
    public AuthResponse refreshToken(RefreshTokenRequest request, HttpServletRequest httpRequest) {
        String refreshTokenValue = request.getRefreshToken();
        String tokenHash = hashToken(refreshTokenValue);
        String currentIp = getClientIp(httpRequest);
        String currentUserAgent = httpRequest.getHeader("User-Agent");

        // Find token
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));

        Session session = storedToken.getSession();
        String phone = session.getUser().getPhone();

        // Check revoked
        if (storedToken.isRevoked()) {
            log.warn("Refresh token revoked for user: {} - Deleting session", phone);
            // commit first, then throw
            transactionTemplate.executeWithoutResult(status -> {
                refreshTokenRepository.deleteBySession(session);
                sessionRepository.delete(session);
            });
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token revoked - login again");
        }

        // Check token expiry
        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            log.warn("Refresh token expired for user: {} - Deleting token", phone);
            transactionTemplate.executeWithoutResult(status -> {
                refreshTokenRepository.delete(storedToken);
            });
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token expired - login again");
        }

        // Check session expiry
        if (session.getExpiresAt().isBefore(Instant.now())) {
            log.warn("Session expired for user: {} - Deleting session", phone);
            transactionTemplate.executeWithoutResult(status -> {
                refreshTokenRepository.deleteBySession(session);
                sessionRepository.delete(session);
            });
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session expired - login again");
        }

        // Check IP
        if (!session.getIpAddress().equals(currentIp)) {
            log.warn("IP mismatch for user: {} - Expected: {}, Got: {} - Deleting session", phone, session.getIpAddress(), currentIp);
            transactionTemplate.executeWithoutResult(status -> {
                refreshTokenRepository.deleteBySession(session);
                sessionRepository.delete(session);
            });
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "IP changed - login again");
        }

        // Check User-Agent
        if (!session.getUserAgent().equals(currentUserAgent)) {
            log.warn("User-Agent mismatch for user: {} - Deleting session", phone);
            transactionTemplate.executeWithoutResult(status -> {
                refreshTokenRepository.deleteBySession(session);
                sessionRepository.delete(session);
            });
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User-Agent changed - login again");
        }

        // Token rotation and success
        return transactionTemplate.execute(status -> {
            // Revoke old token
            storedToken.setRevoked(true);
            refreshTokenRepository.save(storedToken);

            // Create new refresh token
            String newRefreshTokenValue = generateRefreshToken(session);
            RefreshToken newToken = new RefreshToken();
            newToken.setTokenHash(hashToken(newRefreshTokenValue));
            newToken.setSession(session);
            newToken.setRevoked(false);
            newToken.setCreatedAt(Instant.now());
            newToken.setExpiresAt(Instant.now().plusMillis(refreshExp));
            refreshTokenRepository.save(newToken);

            // Update session
            session.setExpiresAt(Instant.now().plusMillis(refreshExp));
            session.setLastActiveAt(Instant.now());
            sessionRepository.save(session);

            // Generate new access token
            String newAccessToken = jwtUtil.generateAccessToken(session.getUser().getId().toString(), session.getId().toString(), accessExp);

            log.info("Token refreshed successfully for user: {}", phone);

            return AuthResponse.builder()
                    .success(true)
                    .statusCode(200)
                    .message("Token refreshed successfully")
                    .data(AuthResponse.AuthData.builder()
                            .accessToken(newAccessToken)
                            .refreshToken(newRefreshTokenValue)
                            .expiresIn(accessExp / 1000)
                            .tokenType("Bearer")
                            .user(mapToUserResponse(session.getUser()))
                            .build())
                    .build();
        });
    }

    @Transactional
    public void logout(String sessionId) {
        try {
            UUID sessionUuid = UUID.fromString(sessionId);
            Session session = sessionRepository.findById(sessionUuid)
                    .orElseThrow(() -> new RuntimeException("Session not found"));

            refreshTokenRepository.deleteBySession(session);
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
        user.setRole(User.Role.USER);
        return userRepository.save(user);
    }

    private Session createSession(User user, HttpServletRequest request) {
        Session session = new Session();
        session.setUser(user);
        session.setIpAddress(getClientIp(request));
        session.setUserAgent(request.getHeader("User-Agent"));
        session.setCreatedAt(Instant.now());
        session.setExpiresAt(Instant.now().plusMillis(refreshExp));
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
        refreshToken.setExpiresAt(Instant.now().plusMillis(refreshExp));

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
                .role(user.getRole().name())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}