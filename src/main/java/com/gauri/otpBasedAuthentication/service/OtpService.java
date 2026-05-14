package com.gauri.otpBasedAuthentication.service;

import com.gauri.otpBasedAuthentication.entity.OtpCode;
import com.gauri.otpBasedAuthentication.repository.OtpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final OtpRepository otpRepository;
    private final ExternalOtpService externalOtpService;

    @Value("${otp.expiration.minutes}")
    private int otpExpirationMinutes;

    @Value("${otp.resend.limit}")
    private int resendLimit;

    @Value("${otp.resend.window.minutes}")
    private int resendWindowMinutes;

    public void generateAndSendOtp(String phone, boolean isNewUser) {

        // fetch OTP from external API
        String otpCode = externalOtpService.fetchOtp(phone);

        // Save to DB
        OtpCode otp = new OtpCode();
        otp.setPhone(phone);
        otp.setOtpCode(otpCode);
        otp.setIsUsed(false);
        otp.setResendCount(0);
        otp.setCreatedAt(Instant.now());
        otp.setExpiresAt(Instant.now().plus(otpExpirationMinutes, ChronoUnit.MINUTES));

        otpRepository.save(otp);
    }

    public boolean verifyOtp(String phone, String otpCode) {
        Optional<OtpCode> otpOpt = otpRepository.findByPhoneAndOtpCodeAndIsUsedFalseAndExpiresAtAfter(
                phone, otpCode, Instant.now()
        );

        if (otpOpt.isEmpty()) {
            return false;
        }

        OtpCode otp = otpOpt.get();
        otp.setIsUsed(true);
        otpRepository.save(otp);

        return true;
    }

    public void resendOtp(String phone) {

        // Find latest unused OTP
        OtpCode latestOtp = otpRepository
                .findTopByPhoneAndIsUsedFalseOrderByCreatedAtDesc(phone)
                .orElseThrow(() -> new RuntimeException(
                        "No active OTP found. Please request a new OTP first"));

        // If expired
        if (latestOtp.getExpiresAt().isBefore(Instant.now())) {
            throw new RuntimeException("OTP expired. Please request a new OTP");
        }

        // Check 1: Per-OTP chain resend count limit
        if (latestOtp.getResendCount() >= resendLimit) {
            throw new RuntimeException("Maximum " + resendLimit +
                    " resends reached for this OTP. Please request a new OTP");
        }

        // Call external API — sends NEW otp to phone
        String newOtpCode = externalOtpService.fetchOtp(phone);

        OtpCode newOtp = new OtpCode();
        newOtp.setPhone(phone);
        newOtp.setOtpCode(newOtpCode);
        newOtp.setIsUsed(false);
        newOtp.setResendCount(latestOtp.getResendCount() + 1);
        newOtp.setCreatedAt(Instant.now());
        newOtp.setExpiresAt(Instant.now().plus(otpExpirationMinutes, ChronoUnit.MINUTES));
        otpRepository.save(newOtp);

        log.info("New OTP created and sent for phone: {}, resendCount: {}",
                phone, newOtp.getResendCount());
    }

    @Transactional
    public void invalidateUnusedOtps(String phone) {
        int updatedCount = otpRepository.invalidateUnusedOtpsByPhone(phone);
        if (updatedCount > 0) {
            log.info("Invalidated {} unused OTPs for phone: {}", updatedCount, phone);
        }
    }

}
