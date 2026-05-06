package com.gauri.otpBasedAuthentication.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {
    private Boolean success;
    private Integer statusCode;
    private String message;
    private AuthData data;

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AuthData {
        private String accessToken;
        private String refreshToken;
        private long expiresIn;
        private String tokenType;
        private UserResponse user;
        private Boolean userExists;
    }
}