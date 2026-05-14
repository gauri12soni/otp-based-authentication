package com.gauri.otpBasedAuthentication.controller;

import com.gauri.otpBasedAuthentication.dto.*;
import com.gauri.otpBasedAuthentication.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;


    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@Valid @RequestBody SendOtpRequest request, HttpServletRequest httpRequest){
        try {
            AuthResponse response = authService.sendOtp(request, httpRequest);
            return ResponseEntity.ok(response);
        }
        catch (Exception e){
           return ResponseEntity.badRequest().body(
                   ErrorResponse.builder()
                           .success(false)
                           .statusCode(400)
                           .error("INVALID_PHONE")
                           .message(e.getMessage())
                           .build()
           );
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody VerifyOtpRequest request, HttpServletRequest httpRequest){
        try {
            AuthResponse response = authService.verifyOtp(request, httpRequest);
            return ResponseEntity.ok(response);
        }
        catch (Exception e){
            return ResponseEntity.badRequest().body(
                    ErrorResponse.builder()
                            .success(false)
                            .statusCode(400)
                            .error("INVALID_OTP")
                            .message(e.getMessage())
                            .build()
            );
        }
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@Valid @RequestBody SendOtpRequest request, HttpServletRequest httpRequest){
        try {
            AuthResponse response = authService.resendOtp(request, httpRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ErrorResponse.builder()
                            .success(false)
                            .statusCode(400)
                            .error("RATE_LIMIT_EXCEEDED")
                            .message(e.getMessage())
                            .build()
            );
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest httpRequest){
        try{
            AuthResponse response = authService.refreshToken(request, httpRequest);
            return ResponseEntity.ok(response);
        }
        catch (Exception e){
            return ResponseEntity.status(401).body(
                    ErrorResponse.builder()
                            .success(false)
                            .statusCode(401)
                            .error("INVALID_REFRESH_TOKEN")
                            .message(e.getMessage())
                            .build()
            );
        }
    }
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request){
        try{
            String sessionId = (String) request.getAttribute("sessionId");
            if(sessionId == null){
                throw new RuntimeException("No session found");
            }
            authService.logout(sessionId);
            return ResponseEntity.ok(
                    AuthResponse.builder()
                            .success(true)
                            .statusCode(200)
                            .message("Logged out successfully")
                            .build()
            );
        }
        catch (Exception e){
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

