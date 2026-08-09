package com.project.kiro.config;

import com.project.kiro.service.EmailSyncService;
import com.project.kiro.service.GmailOAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class SyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(SyncScheduler.class);

    private final GmailOAuthService gmailOAuthService;
    private final EmailSyncService emailSyncService;

    public SyncScheduler(GmailOAuthService gmailOAuthService, EmailSyncService emailSyncService) {
        this.gmailOAuthService = gmailOAuthService;
        this.emailSyncService = emailSyncService;
    }

    /**
     * Automatically syncs Gmail every 15 minutes (configurable).
     * Only runs if a Gmail account is connected.
     * Catches all exceptions to prevent scheduler from stopping.
     */
    @Scheduled(fixedDelayString = "${gmail.sync.interval-minutes:15}", timeUnit = java.util.concurrent.TimeUnit.MINUTES)
    public void scheduledSync() {
        if (!gmailOAuthService.isConnected()) {
            // No account connected — skip silently
            return;
        }

        try {
            log.info("Starting scheduled Gmail sync");
            int imported = emailSyncService.executeSyncCycle();
            log.info("Scheduled sync completed: {} expenses imported", imported);
        } catch (Exception e) {
            log.error("Scheduled Gmail sync failed: {}", e.getMessage());
            // Do not propagate — scheduler should continue at next interval
        }
    }
}
