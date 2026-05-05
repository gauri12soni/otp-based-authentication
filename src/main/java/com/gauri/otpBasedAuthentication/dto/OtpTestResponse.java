package com.gauri.otpBasedAuthentication.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class OtpTestResponse {
    private Boolean success;
    private Integer statusCode;
    private String message;
    private String phone;
    private String otpCode;
    private long expiredIn;
    private Boolean isUsed;
    private Integer resendCount;
}