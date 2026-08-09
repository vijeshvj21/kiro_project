package com.project.kiro.controller;

import com.project.kiro.dto.response.AuthUrlResponse;
import com.project.kiro.dto.response.ConnectionStatusResponse;
import com.project.kiro.dto.response.DisconnectResponse;
import com.project.kiro.dto.response.SyncResponse;
import com.project.kiro.dto.response.SyncStatusResponse;
import com.project.kiro.model.SyncStatus;
import com.project.kiro.repository.SyncStatusRepository;
import com.project.kiro.service.EmailSyncService;
import com.project.kiro.service.GmailOAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequestMapping("/api/v1/email")
public class EmailController {

    private static final Logger log = LoggerFactory.getLogger(EmailController.class);

    private final GmailOAuthService gmailOAuthService;
    private final EmailSyncService emailSyncService;
    private final SyncStatusRepository syncStatusRepository;

    @org.springframework.beans.factory.annotation.Value("${app.frontend-url:http://localhost:5174}")
    private String frontendUrl;

    public EmailController(GmailOAuthService gmailOAuthService,
                           EmailSyncService emailSyncService,
                           SyncStatusRepository syncStatusRepository) {
        this.gmailOAuthService = gmailOAuthService;
        this.emailSyncService = emailSyncService;
        this.syncStatusRepository = syncStatusRepository;
    }

    /**
     * GET /api/v1/email/auth
     * Returns the Google OAuth2 authorization URL.
     */
    @GetMapping("/auth")
    public ResponseEntity<AuthUrlResponse> getAuthUrl() {
        String authUrl = gmailOAuthService.getAuthorizationUrl();
        return ResponseEntity.ok(new AuthUrlResponse(authUrl));
    }

    /**
     * GET /api/v1/email/callback
     * Handles the OAuth2 callback from Google.
     * Exchanges the authorization code for tokens and redirects to frontend.
     */
    @GetMapping("/callback")
    public void handleCallback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "error", required = false) String error,
            HttpServletResponse response) throws IOException {

        if (error != null) {
            log.warn("OAuth callback received error: {}", error);
            response.sendRedirect(frontendUrl + "/expenses?gmail=error&reason=" + error);
            return;
        }

        if (code == null || code.isBlank()) {
            response.sendRedirect(frontendUrl + "/expenses?gmail=error&reason=no_code");
            return;
        }

        try {
            gmailOAuthService.exchangeCodeForTokens(code);
            response.sendRedirect(frontendUrl + "/expenses?gmail=connected");
        } catch (Exception e) {
            log.error("OAuth callback failed: {}", e.getMessage());
            response.sendRedirect(frontendUrl + "/expenses?gmail=error&reason=exchange_failed");
        }
    }

    /**
     * GET /api/v1/email/status
     * Returns the Gmail connection status.
     */
    @GetMapping("/status")
    public ResponseEntity<ConnectionStatusResponse> getStatus() {
        boolean connected = gmailOAuthService.isConnected();
        return ResponseEntity.ok(new ConnectionStatusResponse(connected));
    }

    /**
     * POST /api/v1/email/disconnect
     * Disconnects the Gmail account.
     */
    @PostMapping("/disconnect")
    public ResponseEntity<DisconnectResponse> disconnect() {
        String warning = gmailOAuthService.disconnect();
        String message = "Gmail account disconnected successfully.";
        return ResponseEntity.ok(new DisconnectResponse(message, warning));
    }

    /**
     * POST /api/v1/email/sync
     * Triggers a manual sync. Returns the count of imported expenses.
     */
    @PostMapping("/sync")
    public ResponseEntity<SyncResponse> triggerSync() {
        int importedCount = emailSyncService.executeSyncCycle();
        return ResponseEntity.ok(new SyncResponse(importedCount));
    }

    /**
     * GET /api/v1/email/sync/status
     * Returns the last sync status (timestamp and count).
     */
    @GetMapping("/sync/status")
    public ResponseEntity<SyncStatusResponse> getSyncStatus() {
        var statusOpt = syncStatusRepository.findAll().stream().findFirst();
        if (statusOpt.isEmpty()) {
            return ResponseEntity.ok(new SyncStatusResponse(null, 0));
        }
        SyncStatus status = statusOpt.get();
        return ResponseEntity.ok(new SyncStatusResponse(status.getLastSyncAt(), status.getImportedCount()));
    }
}
