package com.project.kiro.repository;

import com.project.kiro.model.SyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SyncStatusRepository extends JpaRepository<SyncStatus, Long> {
}
