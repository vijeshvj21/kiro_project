# Technical Design Document: Gmail Email Import

## Introduction

This document describes the technical design for the Gmail Email Import feature. The feature integrates with Google's Gmail API via OAuth2 to automatically discover Indian bank debit notification emails, parse transaction details, and persist them as expenses in the existing Expense Tracker application.

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                        React Frontend                                │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │  ExpensesPage                                                   │ │
│  │  ┌──────────────────────────────────────────────────────────┐  │ │
│  │  │  GmailPanel (connect/disconnect/sync/status)             │  │ │
│  │  └──────────────────────────────────────────────────────────┘  │ │
│  └────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
                              │ HTTP
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                   Spring Boot Backend                                 │
│                                                                       │
│  ┌─────────────────┐   ┌──────────────────┐   ┌──────────────────┐ │
│  │ EmailController  │──▶│ GmailOAuthService│──▶│ Google OAuth2 API│ │
│  └─────────────────┘   └──────────────────┘   └──────────────────┘ │
│          │                                                           │
│          ▼                                                           │
│  ┌─────────────────┐   ┌──────────────────┐   ┌──────────────────┐ │
│  │ EmailSyncService │──▶│   EmailParser    │   │ CategoryMatcher  │ │
│  └─────────────────┘   └──────────────────┘   └──────────────────┘ │
│          │                                                           │
│          ▼                                                           │
│  ┌─────────────────┐                                                │
│  │ SyncScheduler   │ (every 15 min)                                 │
│  └─────────────────┘                                                │
│                                                                       │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │ JPA Repositories: GmailTokenRepository,                       │   │
│  │ ProcessedEmailRepository, SyncStatusRepository                │   │
│  └──────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │   H2 / PostgreSQL │
                    └──────────────────┘
```

---

## New Dependencies (pom.xml)

```xml
<!-- Google API Client -->
<dependency>
    <groupId>com.google.api-client</groupId>
    <artifactId>google-api-client</artifactId>
    <version>2.7.2</version>
</dependency>

<!-- Google OAuth2 HTTP Client -->
<dependency>
    <groupId>com.google.auth</groupId>
    <artifactId>google-auth-library-oauth2-http</artifactId>
    <version>1.23.0</version>
</dependency>

<!-- Gmail API -->
<dependency>
    <groupId>com.google.apis</groupId>
    <artifactId>google-api-services-gmail</artifactId>
    <version>v1-rev20240520-2.0.0</version>
</dependency>

<!-- Jackson for Google HTTP Client -->
<dependency>
    <groupId>com.google.http-client</groupId>
    <artifactId>google-http-client-jackson2</artifactId>
    <version>1.44.1</version>
</dependency>
```

---

## Configuration Properties

Added to `application.properties`:

```properties
# Gmail OAuth2
gmail.oauth.client-id=${GMAIL_CLIENT_ID}
gmail.oauth.client-secret=${GMAIL_CLIENT_SECRET}
gmail.oauth.redirect-uri=http://localhost:8080/api/v1/email/callback
gmail.oauth.scopes=https://www.googleapis.com/auth/gmail.readonly

# Sync Configuration
gmail.sync.interval-minutes=15
gmail.sync.initial-lookback-days=30
gmail.sync.timeout-seconds=120

# App timezone
app.timezone=Asia/Kolkata
```

---

## Data Model

### Entity: `GmailToken`

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | Long | PK, auto-generated | Primary key |
| accessToken | String (1024) | NOT NULL | Google access token (encrypted at rest) |
| refreshToken | String (1024) | NOT NULL | Google refresh token (encrypted at rest) |
| connected | boolean | NOT NULL, default true | Whether account is actively connected |
| tokenExpiresAt | Instant | NOT NULL | Expiry timestamp of current access token |
| createdAt | Instant | NOT NULL | When the connection was established |
| updatedAt | Instant | NOT NULL | Last token update timestamp |

**Table**: `gmail_tokens`

```java
package com.project.kiro.model;

@Entity
@Table(name = "gmail_tokens")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class GmailToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "access_token", nullable = false, length = 1024)
    private String accessToken;

    @Column(name = "refresh_token", nullable = false, length = 1024)
    private String refreshToken;

    @Column(nullable = false)
    private boolean connected;

    @Column(name = "token_expires_at", nullable = false)
    private Instant tokenExpiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
```

### Entity: `ProcessedEmail`

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | Long | PK, auto-generated | Primary key |
| gmailMessageId | String (255) | NOT NULL, UNIQUE | Gmail message ID for deduplication |
| processedAt | Instant | NOT NULL | When the email was processed |
| status | String (20) | NOT NULL | EXPENSE_CREATED, CREDIT_SKIPPED, UNPARSEABLE |

**Table**: `processed_emails`  
**Index**: unique index on `gmail_message_id`

```java
package com.project.kiro.model;

@Entity
@Table(name = "processed_emails",
       uniqueConstraints = @UniqueConstraint(columnNames = "gmail_message_id"),
       indexes = @Index(name = "idx_processed_email_msg_id", columnList = "gmail_message_id"))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
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
```

### Entity: `SyncStatus`

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | Long | PK, auto-generated | Primary key (singleton row) |
| lastSyncAt | Instant | nullable | Timestamp of last completed sync |
| importedCount | int | NOT NULL, default 0 | Expenses created in last sync |
| status | String (20) | NOT NULL | SUCCESS, FAILED |

**Table**: `sync_status`

```java
package com.project.kiro.model;

@Entity
@Table(name = "sync_status")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
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
```

---

## Component Design

### 1. `EmailController`

**Package**: `com.project.kiro.controller`  
**Responsibility**: REST endpoint layer for all email-import operations.

| Endpoint | Method | Description | Response |
|----------|--------|-------------|----------|
| `/api/v1/email/auth` | GET | Returns Google OAuth2 authorization URL | `{ "authUrl": "..." }` |
| `/api/v1/email/callback` | GET | Handles OAuth2 callback, exchanges code for tokens | Redirect to frontend with success/error |
| `/api/v1/email/status` | GET | Returns connection status | `{ "connected": true/false }` |
| `/api/v1/email/disconnect` | POST | Revokes token and deletes credentials | `{ "message": "...", "warning": "..." }` |
| `/api/v1/email/sync` | POST | Triggers manual sync | `{ "importedCount": N }` |
| `/api/v1/email/sync/status` | GET | Returns last sync info | `{ "lastSyncAt": "...", "importedCount": N }` |

### 2. `GmailOAuthService`

**Package**: `com.project.kiro.service`  
**Responsibility**: Manages the full OAuth2 lifecycle.

**Key Methods**:
- `String getAuthorizationUrl()` — Builds Google OAuth2 consent URL with `access_type=offline`, `prompt=consent`, scope `gmail.readonly`.
- `void exchangeCodeForTokens(String authCode)` — Exchanges auth code, persists `GmailToken` entity.
- `String getValidAccessToken()` — Returns a non-expired access token; refreshes if expired; throws `GmailNotConnectedException` if refresh fails with `invalid_grant`.
- `void disconnect()` — Calls Google revocation endpoint, deletes `GmailToken` row. Always deletes locally even if revocation fails.
- `boolean isConnected()` — Checks if a valid `GmailToken` row exists with `connected = true`.

**Token Refresh Logic**:
```
if (token.tokenExpiresAt.isBefore(Instant.now())) {
    try {
        newToken = googleTokenRequest(refreshToken)
        update accessToken, tokenExpiresAt
    } catch (InvalidGrantException) {
        delete GmailToken
        throw GmailNotConnectedException
    }
}
```

### 3. `EmailParser`

**Package**: `com.project.kiro.service`  
**Responsibility**: Stateless utility that extracts structured transaction data from raw email text.

**Input**: `EmailContent` record (body text, subject, receivedDate)  
**Output**: `ParsedTransaction` record or `ParseResult.UNPARSEABLE`

```java
public record EmailContent(String body, String subject, LocalDate receivedDate) {}

public record ParsedTransaction(
    BigDecimal amount,
    TransactionType type,  // DEBIT, CREDIT
    LocalDate date,
    String merchant
) {}

public enum TransactionType { DEBIT, CREDIT }
```

**Parsing Rules**:

1. **Amount Extraction** — Regex: `(?:Rs\.?\s*|INR\s*|₹)\s*([\d,]+(?:\.\d{1,2})?)` 
   - Remove all commas, parse as BigDecimal, set scale 2 with HALF_UP
   - If multiple matches: use first, log warning
   - If no match: return UNPARSEABLE

2. **Transaction Type** — Case-insensitive keyword scan:
   - Debit: `debited`, `debit`, `withdrawn`, `paid`, `purchase`, `spent`
   - Credit: `credited`, `credit`, `received`, `refund`
   - If both found: use first occurrence position
   - If neither: return UNPARSEABLE

3. **Date Extraction** — Regex patterns:
   - `(\d{2})[-/](\d{2})[-/](\d{4})` → DD-MM-YYYY or DD/MM/YYYY
   - `(\d{2})\s+(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\s+(\d{4})` → DD MMM YYYY
   - Fallback: use `receivedDate`
   - If future: clamp to today

4. **Merchant Extraction** — Patterns:
   - Look for `at <merchant>`, `to <merchant>`, `towards <merchant>`, `VPA <merchant>`
   - Extract text after keyword until next punctuation or line break
   - Normalise to title case
   - Truncate to 255 chars
   - Fallback: subject line → `"Bank Transaction"`

### 4. `CategoryMatcher`

**Package**: `com.project.kiro.service`  
**Responsibility**: Maps merchant names to categories using keyword rules.

**Rule Table** (applied in order):

| Priority | Category | Keywords |
|----------|----------|----------|
| 1 | Food | Swiggy, Zomato |
| 2 | Transport | Uber, Ola, Rapido, Metro |
| 3 | Shopping | Amazon, Flipkart, Myntra, Meesho |
| 4 | Entertainment | Netflix, Spotify, Hotstar, Youtube, Prime |
| 5 | Healthcare | Pharmacy, Hospital, Clinic, MedPlus, Apollo |

**Logic**:
```java
public Category match(String merchantName) {
    if (merchantName == null || merchantName.isBlank()) {
        return resolveCategory("Other");
    }
    String lower = merchantName.toLowerCase();
    for (CategoryRule rule : RULES) {
        if (rule.keywords().stream().anyMatch(k -> lower.contains(k.toLowerCase()))) {
            return resolveCategory(rule.categoryName());
        }
    }
    return resolveCategory("Other");
}

private Category resolveCategory(String name) {
    return categoryRepository.findByNameLower(name.toLowerCase())
        .orElseGet(() -> categoryRepository.findByNameLower("other")
            .orElseThrow(() -> new IllegalStateException("'Other' category must exist")));
}
```

### 5. `EmailSyncService`

**Package**: `com.project.kiro.service`  
**Responsibility**: Orchestrates the sync cycle — queries Gmail, parses emails, deduplicates, creates expenses.

**Key Methods**:
- `SyncResult executeSyncCycle()` — Full sync flow (used by both manual and scheduled triggers).
- `boolean isSyncInProgress()` — AtomicBoolean guard for concurrency control.

**Sync Flow**:
```
1. Acquire sync lock (AtomicBoolean compareAndSet)
2. Get valid access token from GmailOAuthService
3. Build Gmail query (bank senders + after:timestamp)
4. Fetch message list from Gmail API
5. For each message:
   a. Check ProcessedEmailRepository — skip if exists
   b. Fetch full message content (body + headers)
   c. Parse with EmailParser
   d. If UNPARSEABLE → save ProcessedEmail(UNPARSEABLE), continue
   e. If CREDIT → save ProcessedEmail(CREDIT_SKIPPED), continue
   f. If DEBIT:
      - Match category via CategoryMatcher
      - Create Expense entity
      - Save Expense + ProcessedEmail(EXPENSE_CREATED) in single transaction
6. Update SyncStatus record
7. Release sync lock
```

**Gmail Query Construction**:
```
from:(hdfcbank OR icicibank OR saboricici OR axisbank OR kotakbank OR saborisbi)
  OR from:(alerts@hdfcbank.net OR transaction@icicibank.com OR ...)
after:<lastSyncEpochSeconds>
```

### 6. `SyncScheduler`

**Package**: `com.project.kiro.config`  
**Responsibility**: Triggers background sync at fixed intervals.

```java
@Component
@EnableScheduling
public class SyncScheduler {
    @Scheduled(fixedDelayString = "${gmail.sync.interval-minutes:15}",
               timeUnit = TimeUnit.MINUTES)
    public void scheduledSync() {
        if (!gmailOAuthService.isConnected()) return;
        try {
            emailSyncService.executeSyncCycle();
        } catch (Exception e) {
            log.error("Scheduled sync failed", e);
        }
    }
}
```

---

## API Contracts (DTOs)

### Response DTOs

```java
// GET /api/v1/email/auth
public record AuthUrlResponse(String authUrl) {}

// GET /api/v1/email/status
public record ConnectionStatusResponse(boolean connected) {}

// POST /api/v1/email/disconnect
public record DisconnectResponse(String message, String warning) {}

// POST /api/v1/email/sync
public record SyncResponse(int importedCount) {}

// GET /api/v1/email/sync/status
public record SyncStatusResponse(Instant lastSyncAt, int importedCount) {}
```

### Error Responses

All error responses use the existing `GlobalExceptionHandler` pattern:
```json
{
  "error": "descriptive message",
  "status": 400|401|409|502|504
}
```

**New Exceptions**:
- `GmailNotConnectedException` → 400 Bad Request
- `GmailApiException` → 502 Bad Gateway
- `SyncInProgressException` → 409 Conflict
- `SyncTimeoutException` → 504 Gateway Timeout

---

## Sequence Diagrams

### OAuth2 Connect Flow

```
User → Frontend: Clicks "Connect Gmail"
Frontend → Backend: GET /api/v1/email/auth
Backend → Frontend: { authUrl: "https://accounts.google.com/o/oauth2/..." }
Frontend → Browser: window.location = authUrl
Browser → Google: Authorization request
Google → Browser: Consent screen
User → Google: Grants permission
Google → Backend: GET /api/v1/email/callback?code=AUTH_CODE
Backend → Google: POST token exchange (code → tokens)
Google → Backend: { access_token, refresh_token, expires_in }
Backend → DB: Save GmailToken(accessToken, refreshToken, connected=true)
Backend → Browser: Redirect to frontend /expenses?gmail=connected
Frontend: Shows "Connected" state in GmailPanel
```

### Scheduled Sync Cycle

```
SyncScheduler: @Scheduled triggers
SyncScheduler → GmailOAuthService: isConnected()?
GmailOAuthService → DB: Find GmailToken
DB → GmailOAuthService: token exists (connected=true)
GmailOAuthService → SyncScheduler: true
SyncScheduler → EmailSyncService: executeSyncCycle()
EmailSyncService → GmailOAuthService: getValidAccessToken()
GmailOAuthService: Refresh if expired
EmailSyncService → Gmail API: messages.list(query, after:timestamp)
Gmail API → EmailSyncService: [messageId1, messageId2, ...]
Loop for each messageId:
  EmailSyncService → ProcessedEmailRepo: existsByGmailMessageId(id)?
  ProcessedEmailRepo → EmailSyncService: false (new)
  EmailSyncService → Gmail API: messages.get(id, format=full)
  Gmail API → EmailSyncService: message body + headers
  EmailSyncService → EmailParser: parse(emailContent)
  EmailParser → EmailSyncService: ParsedTransaction(DEBIT, ₹450, date, "Swiggy")
  EmailSyncService → CategoryMatcher: match("Swiggy")
  CategoryMatcher → DB: findByNameLower("food")
  DB → CategoryMatcher: Category(Food)
  CategoryMatcher → EmailSyncService: Category(Food)
  EmailSyncService → DB: Save Expense + ProcessedEmail (single tx)
End Loop
EmailSyncService → DB: Update SyncStatus(timestamp, count)
```

### Manual Sync

```
User → Frontend: Clicks "Sync from Gmail"
Frontend: Disables button
Frontend → Backend: POST /api/v1/email/sync
Backend → EmailSyncService: isSyncInProgress()? → false
Backend → EmailSyncService: executeSyncCycle()
[...same as scheduled sync...]
EmailSyncService → Backend: SyncResult(importedCount=3)
Backend → Frontend: 200 { importedCount: 3 }
Frontend: Shows toast "Imported 3 new expenses"
Frontend: Re-enables button
```

---

## Frontend Component: GmailPanel

**File**: `frontend/src/components/gmail/GmailPanel.jsx`

### State Machine

```
States:
  LOADING     → initial, fetching connection status
  DISCONNECTED → shows "Connect Gmail" button
  CONNECTED   → shows sync controls + last sync info
  SYNCING     → sync in progress, button disabled
  ERROR       → transient error state, recovers to previous

Transitions:
  LOADING → DISCONNECTED (status.connected = false)
  LOADING → CONNECTED (status.connected = true)
  DISCONNECTED → LOADING (after OAuth callback redirect)
  CONNECTED → SYNCING (user clicks "Sync from Gmail")
  SYNCING → CONNECTED (sync completes)
  CONNECTED → DISCONNECTED (user disconnects)
```

### API Module

**File**: `frontend/src/api/email.js`

```javascript
import client from './client';

export const getConnectionStatus = () => client.get('/email/status');
export const getAuthUrl = () => client.get('/email/auth');
export const disconnect = () => client.post('/email/disconnect');
export const triggerSync = () => client.post('/email/sync');
export const getSyncStatus = () => client.get('/email/sync/status');
```

### UI Layout

```
┌─────────────────────────────────────────────┐
│  Gmail Import                                │
│                                              │
│  [When disconnected:]                        │
│  📧 Connect your Gmail to auto-import       │
│     expenses from bank emails.              │
│  [ Connect Gmail ]                           │
│                                              │
│  [When connected:]                           │
│  ✓ Gmail connected                          │
│  Last sync: 2 minutes ago (3 imported)      │
│  [ Sync from Gmail ]  [ Disconnect ]        │
└─────────────────────────────────────────────┘
```

---

## Testing Strategy

### Unit Tests

| Component | Test Focus |
|-----------|-----------|
| EmailParser | Amount extraction (all formats), type detection, date parsing, merchant extraction |
| CategoryMatcher | Keyword matching, priority ordering, null/empty handling, missing category fallback |
| GmailOAuthService | Token refresh logic, invalid_grant handling, disconnect revocation failure |
| EmailSyncService | Deduplication, transaction filtering, error recovery |

### Property-Based Tests (jqwik)

**EmailParser Amount Properties**:
- For any valid amount string with `Rs.`/`INR`/`₹` prefix + digits/commas/decimal → parsed BigDecimal equals expected numeric value
- Commas in any position (Indian grouping: `1,20,000`) are always stripped correctly
- Result always has scale = 2

**EmailParser Type Detection Properties**:
- Any string containing a debit keyword (case variations) → classified as DEBIT
- Any string containing a credit keyword (case variations) → classified as CREDIT
- String with no keywords → UNPARSEABLE

**CategoryMatcher Properties**:
- For any merchant string containing a rule keyword → returns the correct category
- Priority: if string contains keywords from two rules → first rule wins
- Null/empty input → always returns "Other"
- Unknown merchant → always returns "Other"

### Integration Tests

- Full OAuth flow with mocked Google endpoints (MockRestServiceServer)
- End-to-end sync with mocked Gmail API responses
- Deduplication: run sync twice with same messages → no duplicates

---

## Error Handling

| Scenario | HTTP Status | Behaviour |
|----------|-------------|-----------|
| No Gmail connected, sync requested | 400 | Return error message |
| Access token expired, refresh succeeds | — | Transparent to caller |
| Refresh token revoked (invalid_grant) | 401 | Delete tokens, require reconnect |
| Gmail API error during sync | 502 | Log, return error, update SyncStatus |
| Sync already in progress | 409 | Return conflict message |
| Sync timeout (>120s) | 504 | Abort, clear lock, update SyncStatus |
| Email parse failure | — | Save as UNPARSEABLE, continue batch |
| DB error saving ProcessedEmail | — | Roll back expense, log, continue |

---

## File Structure (New Files)

```
src/main/java/com/project/kiro/
├── config/
│   └── SyncScheduler.java
├── controller/
│   └── EmailController.java
├── dto/response/
│   ├── AuthUrlResponse.java
│   ├── ConnectionStatusResponse.java
│   ├── DisconnectResponse.java
│   ├── SyncResponse.java
│   └── SyncStatusResponse.java
├── exception/
│   ├── GmailNotConnectedException.java
│   ├── GmailApiException.java
│   ├── SyncInProgressException.java
│   └── SyncTimeoutException.java
├── model/
│   ├── GmailToken.java
│   ├── ProcessedEmail.java
│   └── SyncStatus.java
├── repository/
│   ├── GmailTokenRepository.java
│   ├── ProcessedEmailRepository.java
│   └── SyncStatusRepository.java
└── service/
    ├── GmailOAuthService.java
    ├── EmailSyncService.java
    ├── EmailParser.java
    └── CategoryMatcher.java

frontend/src/
├── api/
│   └── email.js
└── components/
    └── gmail/
        └── GmailPanel.jsx
```

---

## Traceability Matrix

| Requirement | Components |
|-------------|-----------|
| Req 1: OAuth Connect | GmailOAuthService, EmailController, GmailToken, GmailTokenRepository |
| Req 2: OAuth Disconnect | GmailOAuthService, EmailController |
| Req 3: Amount Extraction | EmailParser |
| Req 4: Transaction Type Detection | EmailParser |
| Req 5: Date Extraction | EmailParser |
| Req 6: Merchant Extraction | EmailParser |
| Req 7: Duplicate Detection | EmailSyncService, ProcessedEmail, ProcessedEmailRepository |
| Req 8: Background Sync | SyncScheduler, EmailSyncService, SyncStatus |
| Req 9: Manual Sync | EmailController, EmailSyncService |
| Req 10: Category Auto-Assignment | CategoryMatcher, CategoryRepository |
| Req 11: Sync Status | EmailSyncService, SyncStatus, SyncStatusRepository |
| Req 12: Frontend Panel | GmailPanel, email.js API module |
| Req 13: Gmail Query Filtering | EmailSyncService |
