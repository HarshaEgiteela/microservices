package com.example.userservice.repository;

import com.example.userservice.entity.Authentication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthenticationRepository
        extends JpaRepository<Authentication, Long> {

    Optional<Authentication> findByUserId(Long userId);
}