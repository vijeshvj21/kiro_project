package com.project.kiro.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "sync_status")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "last_sync_at")
    private Instant lastSyncAt;

    @Column(name = "imported_count", nullable = false)
    private int importedCount;

    @Column(nullable = false, length = 20)
    private String status; // SUCCESS, FAILED
}
