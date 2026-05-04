package com.gauri.otpBasedAuthentication.service;

import com.gauri.otpBasedAuthentication.entity.OtpCode;
import com.gauri.otpBasedAuthentication.repository.OtpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final OtpRepository otpRepository;
    private final SmsService smsService;

    @Value("${otp.expiration.minutes}")
    private int otpExpirationMinutes;

    @Value("${otp.resend.limit}")
    private int resendLimit;

    @Value("${otp.resend.window.minutes}")
    private int resendWindowMinutes;

    private static final Random random = new Random();

    public String generateAndSendOtp(String phone, boolean isNewUser) {
        String otpCode = String.format("%06d", random.nextInt(1000000));

        // Create new OTP
        OtpCode otp = new OtpCode();
        otp.setPhone(phone);
        otp.setOtpCode(otpCode);
        otp.setIsUsed(false);
        otp.setResendCount(0);
        otp.setCreatedAt(LocalDateTime.now());
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(otpExpirationMinutes));

        otpRepository.save(otp);

        // Send SMS
        boolean sent = smsService.sendOtp(phone, otpCode);

        if (!sent) {
            throw new RuntimeException("Failed to send OTP");
        }

        return otpCode;
    }

    public boolean verifyOtp(String phone, String otpCode) {
        Optional<OtpCode> otpOpt = otpRepository.findByPhoneAndOtpCodeAndIsUsedFalseAndExpiresAtAfter(
                phone, otpCode, LocalDateTime.now()
        );

        if (otpOpt.isEmpty()) {
            return false;
        }

        OtpCode otp = otpOpt.get();
        otp.setIsUsed(true);
        otpRepository.save(otp);

        return true;
    }

    public String resendOtp(String phone) {
        // Check rate limit
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(resendWindowMinutes);
        long resendCount = otpRepository.countByPhoneAndCreatedAtAfter(phone, windowStart);

        if (resendCount >= resendLimit) {
            throw new RuntimeException("Rate limit exceeded. Maximum " + resendLimit + " resends per " + resendWindowMinutes + " minutes");
        }

        // Find latest OTP
        Optional<OtpCode> latestOtpOpt = otpRepository.findTopByPhoneOrderByCreatedAtDesc(phone);

        if (latestOtpOpt.isEmpty()) {
            throw new RuntimeException("No OTP found for this phone number");
        }

        OtpCode latestOtp = latestOtpOpt.get();

        // Check if not expired
        if (latestOtp.getExpiresAt().isBefore(LocalDateTime.now())) {
            // Generate new OTP if expired
            return generateAndSendOtp(phone, false);
        }

        // Increment resend count and send same OTP
        latestOtp.setResendCount(latestOtp.getResendCount() + 1);
        latestOtp.setCreatedAt(LocalDateTime.now());
        latestOtp.setExpiresAt(LocalDateTime.now().plusMinutes(otpExpirationMinutes));
        otpRepository.save(latestOtp);

        boolean sent = smsService.sendOtp(phone, latestOtp.getOtpCode());
        if (!sent) {
            throw new RuntimeException("Failed to send OTP");
        }

        return latestOtp.getOtpCode();
    }

}
