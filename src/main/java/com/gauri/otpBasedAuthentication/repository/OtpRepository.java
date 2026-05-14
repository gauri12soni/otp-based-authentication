package com.gauri.otpBasedAuthentication.repository;

import com.gauri.otpBasedAuthentication.entity.OtpCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    Optional<OtpCode> findTopByPhoneAndIsUsedFalseOrderByCreatedAtDesc(String phone);

    @Modifying
    @Query("UPDATE OtpCode o SET o.isUsed = true WHERE o.phone = :phone AND o.isUsed = false")
    int invalidateUnusedOtpsByPhone(@Param("phone") String phone);

    @Modifying
    @Transactional
    int deleteByExpiresAtBefore(Instant now);

    long countByPhoneAndCreatedAtAfter(String phone, Instant time);
}