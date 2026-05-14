package com.gauri.otpBasedAuthentication.repository;

import com.gauri.otpBasedAuthentication.entity.Session;
import com.gauri.otpBasedAuthentication.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;


import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionRepository extends JpaRepository<Session, UUID> {

    Optional<Session> findByUser(User user);
    Optional<Session> findById(UUID sessionId);

}
