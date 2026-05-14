package com.gauri.otpBasedAuthentication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class VerifyOtpRequest {
        @NotBlank
        @Pattern(regexp = "^\\+[1-9][0-9]{7,14}$", message = "Invalid phone number format")
        private String phone;

        @NotBlank(message= "OTP is required")
        @Pattern(regexp = "^\\d{6}$", message = "OTP must be 6 digits")
        private String otpCode;
}
