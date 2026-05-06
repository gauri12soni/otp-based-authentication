package com.gauri.otpBasedAuthentication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;



@Data
public class SendOtpRequest {
    @NotBlank(message= "Phone number is required")
    @Pattern(regexp = "^\\+91[6-9][0-9]{9}$", message = "Invalid phone number format")
    private String phone;
}