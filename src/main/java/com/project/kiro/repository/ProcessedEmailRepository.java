package com.project.kiro.repository;

import com.project.kiro.model.ProcessedEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedEmailRepository extends JpaRepository<ProcessedEmail, Long> {
    boolean existsByGmailMessageId(String gmailMessageId);
}
