package com.gauri.otpBasedAuthentication.repository;

import com.gauri.otpBasedAuthentication.entity.OtpCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

public interface OtpRepository extends JpaRepository<OtpCode, Long> {
    Optional<OtpCode> findByPhoneAndOtpCodeAndIsUsedFalseAndExpiresAtAfter(
            String phone, String otpCode, Instant now);

    Optional<OtpCode> findTopByPhoneOrderByCreatedAtDesc(String phone);
    long countByPhoneAndResendCountGreaterThanAndCreatedAtAfter(
            String phone, int resendCount, Instant time);

    // Add this method — used in resendOtp()
    Optional<OtpCode> findTopByPhoneAndIsUsedFalseOrderByCreatedAtDesc(String phone);


    @Modifying
    @Transactional
    void deleteByExpiresAtBefore(Instant now);

    long countByPhoneAndCreatedAtAfter(String phone, Instant time);
}