package com.project.kiro.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "processed_emails",
       uniqueConstraints = @UniqueConstraint(columnNames = "gmail_message_id"),
       indexes = @Index(name = "idx_processed_email_msg_id", columnList = "gmail_message_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "gmail_message_id", nullable = false, length = 255)
    private String gmailMessageId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    @Column(nullable = false, length = 20)
    private String status; // EXPENSE_CREATED, CREDIT_SKIPPED, UNPARSEABLE
}
