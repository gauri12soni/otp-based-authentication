package com.gauri.otpBasedAuthentication.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalOtpService {

    private final RestTemplate restTemplate;

    @Value("${external.otp.api.url}")
    private String externalOtpApiUrl;

    public String fetchOtp(String phone) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Remove +91 for this external API — it expects plain 10 digit number
            String plainPhone = phone.replace("+91", "");

            Map<String, String> body = Map.of("phone", plainPhone);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    externalOtpApiUrl,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            log.info("External OTP API response: {}", response.getBody());

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // Extract OTP from response field "response"
                String otp = (String) response.getBody().get("response");

                if (otp == null || otp.isBlank()) {
                    throw new RuntimeException("OTP not found in external API response");
                }

                log.info("OTP fetched successfully for phone: {}", plainPhone);
                return otp;
            }

            throw new RuntimeException("External OTP API returned unexpected status: "
                    + response.getStatusCode());

        } catch (Exception e) {
            log.error("Failed to fetch OTP from external API for {}: {}", phone, e.getMessage());
            throw new RuntimeException("Failed to fetch OTP: " + e.getMessage());
        }
    }
}