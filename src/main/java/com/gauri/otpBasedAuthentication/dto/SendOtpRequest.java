package com.gauri.otpBasedAuthentication.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;



@Data
public class SendOtpRequest {
    @Pattern(regexp = "^\\+[1-9][0-9]{7,14}$", message = "Invalid phone number format")
    private String phone;
}