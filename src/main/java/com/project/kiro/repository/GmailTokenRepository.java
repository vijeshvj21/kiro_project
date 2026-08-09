package com.project.kiro.repository;

import com.project.kiro.model.GmailToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GmailTokenRepository extends JpaRepository<GmailToken, Long> {
    Optional<GmailToken> findByConnectedTrue();
}
