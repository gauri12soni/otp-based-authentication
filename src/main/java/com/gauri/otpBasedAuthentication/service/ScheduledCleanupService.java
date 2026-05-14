package com.gauri.otpBasedAuthentication.service;

import com.gauri.otpBasedAuthentication.repository.OtpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledCleanupService {

    private final OtpRepository otpRepository;

    @Scheduled(cron = "0 0 2 * * *") // runs every day at 2 AM
    @Transactional
    public void cleanupExpiredOtps() {
        int deleted = otpRepository.deleteByExpiresAtBefore(Instant.now());
        log.info("Scheduled cleanup — deleted {} expired OTPs", deleted);
    }
}