package com.gauri.otpBasedAuthentication.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@Slf4j
public class SmsService {

    @Value("${msg91.auth.key}")
    private String authKey;

    @Value("${msg91.template.id}")
    private String templateId;

    @Value("${msg91.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate;

    public SmsService() {
        this.restTemplate = new RestTemplate();
    }

    public boolean sendOtp(String phone, String otp) {
        try {
            // Remove '+' from phone if present for MSG91
            String mobile = phone.replace("+", "");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("authkey", authKey);

            Map<String, String> body = Map.of(
                    "template_id", templateId,
                    "mobile", mobile,
                    "otp", otp
            );

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            log.info("MSG91 Response: {}", response.getBody());
            return response.getStatusCode() == HttpStatus.OK;

        } catch (Exception e) {
            log.error("Failed to send OTP via MSG91: {}", e.getMessage());
            return false;
        }
    }
}