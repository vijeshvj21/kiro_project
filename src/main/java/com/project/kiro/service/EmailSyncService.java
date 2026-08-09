package com.project.kiro.service;

import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.project.kiro.exception.GmailApiException;
import com.project.kiro.exception.GmailNotConnectedException;
import com.project.kiro.exception.SyncInProgressException;
import com.project.kiro.exception.SyncTimeoutException;
import com.project.kiro.model.Expense;
import com.project.kiro.model.ProcessedEmail;
import com.project.kiro.model.SyncStatus;
import com.project.kiro.repository.ExpenseRepository;
import com.project.kiro.repository.ProcessedEmailRepository;
import com.project.kiro.repository.SyncStatusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class EmailSyncService {

    private static final Logger log = LoggerFactory.getLogger(EmailSyncService.class);

    private final GmailOAuthService gmailOAuthService;
    private final EmailParser emailParser;
    private final CategoryMatcher categoryMatcher;
    private final ExpenseRepository expenseRepository;
    private final ProcessedEmailRepository processedEmailRepository;
    private final SyncStatusRepository syncStatusRepository;

    private final AtomicBoolean syncInProgress = new AtomicBoolean(false);

    @Value("${gmail.sync.initial-lookback-days:30}")
    private int initialLookbackDays;

    @Value("${gmail.sync.timeout-seconds:120}")
    private int timeoutSeconds;

    private static final ZoneId APP_TIMEZONE = ZoneId.of("Asia/Kolkata");

    // Known Indian bank sender email patterns
    private static final List<String> BANK_SENDERS = List.of(
        "alerts@hdfcbank.net",
        "alerts@icicibank.com",
        "transaction@icicibank.com",
        "alerts@axisbank.com",
        "alerts@kotak.com",
        "alerts@sbi.co.in",
        "donotreply@hdfcbank.net",
        "noreply@axisbank.com",
        "alerts@kotakbank.com",
        "alerts@indusind.com",
        "alerts@yesbank.in",
        "alerts@federalbank.co.in",
        "alerts@idfcfirstbank.com",
        "noreply@hdfcbank.net",
        "creditcards@hdfcbank.net",
        "alerts@unionbankofindia.bank",
        "noreply@icicibank.com",
        "alerts@rblbank.com"
    );

    // Additional keyword-based search terms for broader matching
    private static final String KEYWORD_QUERY =
        "subject:(debited OR credited OR transaction OR debit OR credit OR withdrawn OR \"account statement\")";

    public EmailSyncService(
            GmailOAuthService gmailOAuthService,
            EmailParser emailParser,
            CategoryMatcher categoryMatcher,
            ExpenseRepository expenseRepository,
            ProcessedEmailRepository processedEmailRepository,
            SyncStatusRepository syncStatusRepository) {
        this.gmailOAuthService = gmailOAuthService;
        this.emailParser = emailParser;
        this.categoryMatcher = categoryMatcher;
        this.expenseRepository = expenseRepository;
        this.processedEmailRepository = processedEmailRepository;
        this.syncStatusRepository = syncStatusRepository;
    }

    /**
     * Checks if a sync is currently in progress.
     */
    public boolean isSyncInProgress() {
        return syncInProgress.get();
    }

    /**
     * Builds the Gmail search query string.
     * Uses both sender-based filtering and keyword-based filtering for broader coverage.
     * Matches: emails from known bank senders OR emails with transaction keywords in subject.
     */
    String buildGmailQuery() {
        // Get last sync timestamp
        Instant afterTimestamp = getLastSyncTimestamp();

        // Build sender filter: from:(addr1 OR addr2 OR ...)
        StringBuilder senderFilter = new StringBuilder("from:(");
        for (int i = 0; i < BANK_SENDERS.size(); i++) {
            if (i > 0) senderFilter.append(" OR ");
            senderFilter.append(BANK_SENDERS.get(i));
        }
        senderFilter.append(")");

        // Combine: (sender filter OR keyword filter)
        StringBuilder query = new StringBuilder("{");
        query.append(senderFilter);
        query.append(" ");
        query.append(KEYWORD_QUERY);
        query.append("}");

        // Add time filter
        if (afterTimestamp != null) {
            query.append(" after:").append(afterTimestamp.getEpochSecond());
        } else {
            // Initial lookback: 30 days
            Instant lookback = Instant.now().minusSeconds((long) initialLookbackDays * 24 * 60 * 60);
            query.append(" after:").append(lookback.getEpochSecond());
        }

        return query.toString();
    }

    /**
     * Gets the last sync timestamp, or null if never synced.
     */
    private Instant getLastSyncTimestamp() {
        return syncStatusRepository.findAll().stream()
            .findFirst()
            .map(SyncStatus::getLastSyncAt)
            .orElse(null);
    }

    /**
     * Creates a Gmail API service instance using the current valid access token.
     */
    Gmail createGmailService() {
        String accessToken = gmailOAuthService.getValidAccessToken();
        GoogleCredentials credentials = GoogleCredentials.create(
            new AccessToken(accessToken, new Date(Instant.now().plusSeconds(3600).toEpochMilli()))
        );
        return new Gmail.Builder(
            new NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            new HttpCredentialsAdapter(credentials)
        ).setApplicationName("Kiro Expense Tracker").build();
    }

    /**
     * Executes a full sync cycle with timeout and concurrency control.
     * Returns the count of expenses imported.
     */
    public int executeSyncCycle() {
        if (!gmailOAuthService.isConnected()) {
            throw new GmailNotConnectedException(
                "No Gmail account connected. Please connect your Gmail account first.");
        }

        if (!syncInProgress.compareAndSet(false, true)) {
            throw new SyncInProgressException("A sync is already in progress.");
        }

        try {
            // Run sync with timeout
            var future = CompletableFuture.supplyAsync(this::performSync);
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.error("Sync timed out after {} seconds", timeoutSeconds);
            updateSyncStatusFailed();
            throw new SyncTimeoutException("Sync timed out after " + timeoutSeconds + " seconds.");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof GmailNotConnectedException gnce) throw gnce;
            if (cause instanceof SyncInProgressException sipe) throw sipe;
            if (cause instanceof GmailApiException gae) throw gae;
            throw new GmailApiException("Sync failed: " + cause.getMessage(), cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GmailApiException("Sync was interrupted", e);
        } finally {
            syncInProgress.set(false);
        }
    }

    /**
     * Internal sync logic — orchestrates fetching, parsing, deduplicating, and saving expenses.
     */
    private int performSync() {
        int importedCount = 0;
        try {
            Gmail gmailService = createGmailService();
            String query = buildGmailQuery();
            log.info("Executing Gmail sync with query: {}", query);

            // Fetch message list
            ListMessagesResponse response = gmailService.users().messages()
                .list("me")
                .setQ(query)
                .execute();

            List<Message> messages = response.getMessages();
            if (messages == null || messages.isEmpty()) {
                log.info("No new bank emails found");
                updateSyncStatus(0);
                return 0;
            }

            log.info("Found {} potential bank emails to process", messages.size());

            for (Message messageMeta : messages) {
                try {
                    importedCount += processMessage(gmailService, messageMeta.getId());
                } catch (Exception e) {
                    log.error("Error processing message {}: {}", messageMeta.getId(), e.getMessage());
                    // Continue to next message
                }
            }

            updateSyncStatus(importedCount);
            log.info("Sync completed. Imported {} expenses", importedCount);
        } catch (GmailNotConnectedException | SyncInProgressException e) {
            throw e; // Re-throw these
        } catch (Exception e) {
            log.error("Gmail sync cycle failed: {}", e.getMessage(), e);
            updateSyncStatusFailed();
            throw new GmailApiException("Gmail sync failed: " + e.getMessage(), e);
        }
        return importedCount;
    }

    private int processMessage(Gmail gmailService, String messageId) throws Exception {
        // Check deduplication
        if (processedEmailRepository.existsByGmailMessageId(messageId)) {
            log.debug("Skipping already processed message: {}", messageId);
            return 0;
        }

        // Fetch full message
        Message fullMessage = gmailService.users().messages()
            .get("me", messageId)
            .setFormat("full")
            .execute();

        // Extract email content
        EmailContent emailContent = extractEmailContent(fullMessage);
        if (emailContent == null) {
            saveProcessedEmail(messageId, "UNPARSEABLE");
            return 0;
        }

        // Parse the email
        var parsedOpt = emailParser.parse(emailContent);
        if (parsedOpt.isEmpty()) {
            saveProcessedEmail(messageId, "UNPARSEABLE");
            return 0;
        }

        ParsedTransaction parsed = parsedOpt.get();

        // Create expense for both debit and credit transactions
        return createExpenseFromTransaction(messageId, parsed, emailContent.body());
    }

    @Transactional
    protected int createExpenseFromTransaction(String messageId, ParsedTransaction parsed) {
        return createExpenseFromTransaction(messageId, parsed, null);
    }

    @Transactional
    protected int createExpenseFromTransaction(String messageId, ParsedTransaction parsed, String emailBody) {
        try {
            // Try matching category from merchant name first, then from full email body
            var category = categoryMatcher.match(parsed.merchant());
            if (category.getNameLower().equals("other") && emailBody != null) {
                // Fallback: try matching from full email body
                var bodyCategory = categoryMatcher.matchFromBody(emailBody);
                if (!bodyCategory.getNameLower().equals("other")) {
                    category = bodyCategory;
                }
            }

            Expense expense = Expense.builder()
                .amount(parsed.amount())
                .expenseDate(parsed.date())
                .category(category)
                .description(parsed.merchant())
                .transactionType(parsed.type().name())
                .build();

            expenseRepository.save(expense);
            saveProcessedEmail(messageId, "EXPENSE_CREATED");
            return 1;
        } catch (Exception e) {
            log.error("Failed to create expense for message {}: {}", messageId, e.getMessage());
            throw e; // Transaction will rollback
        }
    }

    private void saveProcessedEmail(String messageId, String status) {
        ProcessedEmail processed = ProcessedEmail.builder()
            .gmailMessageId(messageId)
            .processedAt(Instant.now())
            .status(status)
            .build();
        processedEmailRepository.save(processed);
    }

    private EmailContent extractEmailContent(Message message) {
        try {
            // Extract subject from headers
            String subject = "";
            LocalDate receivedDate = LocalDate.now(APP_TIMEZONE);

            if (message.getPayload() != null && message.getPayload().getHeaders() != null) {
                for (MessagePartHeader header : message.getPayload().getHeaders()) {
                    if ("Subject".equalsIgnoreCase(header.getName())) {
                        subject = header.getValue();
                    }
                }
            }

            // Use internalDate for received date
            if (message.getInternalDate() != null) {
                receivedDate = Instant.ofEpochMilli(message.getInternalDate())
                    .atZone(APP_TIMEZONE)
                    .toLocalDate();
            }

            // Extract body text
            String body = extractBodyText(message.getPayload());
            if (body == null || body.isBlank()) {
                return null;
            }

            return new EmailContent(body, subject, receivedDate);
        } catch (Exception e) {
            log.warn("Failed to extract email content: {}", e.getMessage());
            return null;
        }
    }

    private String extractBodyText(MessagePart payload) {
        if (payload == null) return null;

        // Direct body data (simple messages)
        if (payload.getBody() != null && payload.getBody().getData() != null) {
            return decodeBase64(payload.getBody().getData());
        }

        // Multipart — look for text/plain or text/html
        if (payload.getParts() != null) {
            for (MessagePart part : payload.getParts()) {
                if ("text/plain".equals(part.getMimeType()) && part.getBody() != null && part.getBody().getData() != null) {
                    return decodeBase64(part.getBody().getData());
                }
            }
            // Fallback to text/html if no plain text
            for (MessagePart part : payload.getParts()) {
                if ("text/html".equals(part.getMimeType()) && part.getBody() != null && part.getBody().getData() != null) {
                    String html = decodeBase64(part.getBody().getData());
                    return html.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
                }
            }
        }

        return null;
    }

    private String decodeBase64(String data) {
        byte[] bytes = Base64.getUrlDecoder().decode(data);
        return new String(bytes);
    }

    private void updateSyncStatus(int importedCount) {
        SyncStatus status = syncStatusRepository.findAll().stream()
            .findFirst()
            .orElse(SyncStatus.builder().build());

        status.setLastSyncAt(Instant.now());
        status.setImportedCount(importedCount);
        status.setStatus("SUCCESS");
        syncStatusRepository.save(status);
    }

    /**
     * Updates the SyncStatus record with FAILED status and current timestamp.
     */
    private void updateSyncStatusFailed() {
        try {
            SyncStatus status = syncStatusRepository.findAll().stream()
                .findFirst()
                .orElse(SyncStatus.builder().importedCount(0).build());
            status.setLastSyncAt(Instant.now());
            status.setImportedCount(0);
            status.setStatus("FAILED");
            syncStatusRepository.save(status);
        } catch (Exception ex) {
            log.error("Failed to update sync status after timeout", ex);
        }
    }
}
