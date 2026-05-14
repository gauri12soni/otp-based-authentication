package com.gauri.otpBasedAuthentication.repository;

import com.gauri.otpBasedAuthentication.entity.RefreshToken;
import com.gauri.otpBasedAuthentication.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);
    Optional<RefreshToken> findBySession(Session session);

    void deleteBySession(Session session);

}


