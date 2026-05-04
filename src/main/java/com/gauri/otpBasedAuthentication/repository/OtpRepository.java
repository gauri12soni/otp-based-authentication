package com.gauri.otpBasedAuthentication.repository;

import com.gauri.otpBasedAuthentication.entity.OtpCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OtpRepository extends JpaRepository<OtpCode, Long> {
    Optional<OtpCode> findByPhoneAndOtpCodeAndIsUsedFalseAndExpiresAtAfter(
            String phone, String otpCode, LocalDateTime now);

    Optional<OtpCode> findTopByPhoneOrderByCreatedAtDesc(String phone);


    @Modifying
    @Transactional
    void deleteByExpiresAtBefore(LocalDateTime now);

    long countByPhoneAndCreatedAtAfter(String phone, LocalDateTime time);
}