package com.gauri.otpBasedAuthentication.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ErrorResponse {
    private Boolean success;
    private Integer statusCode;
    private String error;
    private String message;
}
