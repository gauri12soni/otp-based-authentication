package com.gauri.otpBasedAuthentication.repository;

import com.gauri.otpBasedAuthentication.entity.Session;
import com.gauri.otpBasedAuthentication.entity.User;
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
public interface SessionRepository extends JpaRepository<Session, UUID> {

    Optional<Session> findByUser(User user);

//    Optional<Session> findByIdAndIsActiveTrue(UUID id);
//
////    @Modifying
////    @Transactional
////    @Query("UPDATE Session s SET s.isActive = false WHERE s.user = :user")
////    void deactivateAllUserSessions(User user);
////
////    @Modifying
////    @Transactional
////    void deleteByExpiresAtBefore(Instant now);


}
