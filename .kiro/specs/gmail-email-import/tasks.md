# Implementation Plan: Gmail Email Import

## Overview

This plan implements the Gmail Email Import feature in incremental steps, starting with foundational dependencies and data models, then building services (parser, matcher, OAuth, sync), wiring the controller, adding the frontend panel, and finishing with integration tests. Each task builds on the previous, ensuring no orphaned code.

## Tasks

- [x] 1. Add dependencies and configuration
  - [x] 1.1 Add Google API dependencies to pom.xml
    - Add `google-api-client`, `google-auth-library-oauth2-http`, `google-api-services-gmail`, and `google-http-client-jackson2` dependencies with versions specified in design
    - _Requirements: 1.1, 13.1_

  - [x] 1.2 Add Gmail OAuth and sync configuration properties
    - Add `gmail.oauth.client-id`, `gmail.oauth.client-secret`, `gmail.oauth.redirect-uri`, `gmail.oauth.scopes` properties
    - Add `gmail.sync.interval-minutes`, `gmail.sync.initial-lookback-days`, `gmail.sync.timeout-seconds` properties
    - Add `app.timezone=Asia/Kolkata` property
    - _Requirements: 1.1, 8.1, 13.3_

- [x] 2. Create data model entities and repositories
  - [x] 2.1 Create GmailToken entity
    - Create `com.project.kiro.model.GmailToken` JPA entity with fields: id, accessToken (1024), refreshToken (1024), connected, tokenExpiresAt, createdAt, updatedAt
    - Use Lombok `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`
    - Table name: `gmail_tokens`
    - _Requirements: 1.2, 1.7_

  - [x] 2.2 Create ProcessedEmail entity
    - Create `com.project.kiro.model.ProcessedEmail` JPA entity with fields: id, gmailMessageId (255, unique), processedAt, status (20)
    - Add unique constraint and index on `gmail_message_id`
    - _Requirements: 7.1, 7.4_

  - [x] 2.3 Create SyncStatus entity
    - Create `com.project.kiro.model.SyncStatus` JPA entity with fields: id, lastSyncAt (nullable), importedCount (default 0), status (20)
    - Table name: `sync_status`
    - _Requirements: 11.1, 11.3_

  - [x] 2.4 Create GmailTokenRepository
    - Create `com.project.kiro.repository.GmailTokenRepository` extending JpaRepository
    - Add method `Optional<GmailToken> findByConnectedTrue()`
    - _Requirements: 1.2, 1.7, 1.8_

  - [x] 2.5 Create ProcessedEmailRepository
    - Create `com.project.kiro.repository.ProcessedEmailRepository` extending JpaRepository
    - Add method `boolean existsByGmailMessageId(String gmailMessageId)`
    - _Requirements: 7.2, 7.4_

  - [x] 2.6 Create SyncStatusRepository
    - Create `com.project.kiro.repository.SyncStatusRepository` extending JpaRepository
    - _Requirements: 11.3, 11.4_

- [x] 3. Create custom exceptions and response DTOs
  - [x] 3.1 Create custom exceptions
    - Create `GmailNotConnectedException` (maps to 400)
    - Create `GmailApiException` (maps to 502)
    - Create `SyncInProgressException` (maps to 409)
    - Create `SyncTimeoutException` (maps to 504)
    - Register exception handlers in `GlobalExceptionHandler`
    - _Requirements: 1.4, 9.3, 9.4, 9.5, 9.7_

  - [x] 3.2 Create response DTOs
    - Create `AuthUrlResponse` record with `authUrl` field
    - Create `ConnectionStatusResponse` record with `connected` field
    - Create `DisconnectResponse` record with `message` and `warning` fields
    - Create `SyncResponse` record with `importedCount` field
    - Create `SyncStatusResponse` record with `lastSyncAt` and `importedCount` fields
    - _Requirements: 1.1, 1.8, 2.1, 9.2, 11.1_

- [x] 4. Checkpoint - Ensure project compiles
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Implement EmailParser service
  - [x] 5.1 Create EmailParser with amount extraction
    - Create `com.project.kiro.service.EmailParser` as a `@Service`
    - Define `EmailContent` record (body, subject, receivedDate) and `ParsedTransaction` record (amount, type, date, merchant)
    - Define `TransactionType` enum (DEBIT, CREDIT)
    - Implement amount extraction regex: `(?:Rs\.?\s*|INR\s*|₹)\s*([\d,]+(?:\.\d{1,2})?)` — strip commas, parse BigDecimal with scale 2 HALF_UP
    - Use first match when multiple amounts found, log warning
    - Return UNPARSEABLE if no amount found
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7_

  - [x] 5.2 Add transaction type detection to EmailParser
    - Implement case-insensitive keyword scan for debit indicators (debited, debit, withdrawn, paid, purchase, spent) and credit indicators (credited, credit, received, refund)
    - If both found, use first occurrence position
    - Return UNPARSEABLE if neither keyword found
    - _Requirements: 4.1, 4.2, 4.4, 4.5_

  - [x] 5.3 Add date extraction to EmailParser
    - Implement regex patterns for DD-MM-YYYY, DD/MM/YYYY, DD MMM YYYY
    - Fallback to email receivedDate if no date found in body
    - Clamp future dates to current day using Asia/Kolkata timezone
    - Convert all dates to LocalDate
    - _Requirements: 5.1, 5.2, 5.3, 5.4_

  - [x] 5.4 Add merchant extraction to EmailParser
    - Look for patterns: `at <merchant>`, `to <merchant>`, `towards <merchant>`, `VPA <merchant>`
    - Extract until punctuation or line break
    - Normalise to title case, truncate to 255 characters
    - Fallback chain: body extraction → subject line → "Bank Transaction"
    - _Requirements: 6.1, 6.2, 6.3, 6.4_

  - [x]* 5.5 Write property tests for EmailParser amount extraction
    - **Property 1: Valid amount strings with Rs./INR/₹ prefix + digits/commas/decimal → parsed BigDecimal equals expected numeric value**
    - **Property 2: Commas in any position (Indian grouping 1,20,000) are always stripped correctly**
    - **Property 3: Result always has scale = 2**
    - **Validates: Requirements 3.1, 3.2, 3.3, 3.5, 3.6**

  - [x]* 5.6 Write property tests for EmailParser transaction type detection
    - **Property 4: Any string containing a debit keyword (case variations) → classified as DEBIT**
    - **Property 5: Any string containing a credit keyword (case variations) → classified as CREDIT**
    - **Property 6: String with no keywords → UNPARSEABLE**
    - **Validates: Requirements 4.1, 4.2, 4.4, 4.5**

- [x] 6. Implement CategoryMatcher service
  - [x] 6.1 Create CategoryMatcher with keyword rules
    - Create `com.project.kiro.service.CategoryMatcher` as a `@Service`
    - Define ordered rule table: Food (Swiggy, Zomato) → Transport (Uber, Ola, Rapido, Metro) → Shopping (Amazon, Flipkart, Myntra, Meesho) → Entertainment (Netflix, Spotify, Hotstar, Youtube, Prime) → Healthcare (Pharmacy, Hospital, Clinic, MedPlus, Apollo)
    - Implement case-insensitive contains matching with first-match-wins priority
    - Null/empty merchant → return "Other"
    - Resolve category by case-insensitive DB lookup; fallback to "Other" if not found
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5, 10.6, 10.7, 10.8, 10.9, 10.10_

  - [x]* 6.2 Write property tests for CategoryMatcher
    - **Property 7: For any merchant string containing a rule keyword → returns the correct category**
    - **Property 8: If string contains keywords from two rules → first rule in priority wins**
    - **Property 9: Null/empty input → always returns "Other"**
    - **Property 10: Unknown merchant (no keyword match) → always returns "Other"**
    - **Validates: Requirements 10.1, 10.2, 10.3, 10.4, 10.5, 10.6, 10.7, 10.8**

- [x] 7. Implement GmailOAuthService
  - [x] 7.1 Create GmailOAuthService with authorization URL generation
    - Create `com.project.kiro.service.GmailOAuthService` as a `@Service`
    - Inject OAuth config properties (client-id, client-secret, redirect-uri, scopes)
    - Implement `getAuthorizationUrl()` — build Google OAuth2 consent URL with `access_type=offline`, `prompt=consent`, scope `gmail.readonly`
    - Implement `isConnected()` — check if GmailToken exists with `connected=true`
    - _Requirements: 1.1, 1.7, 1.8_

  - [x] 7.2 Add token exchange and refresh logic to GmailOAuthService
    - Implement `exchangeCodeForTokens(String authCode)` — call Google token endpoint, persist GmailToken with accessToken, refreshToken, tokenExpiresAt, connected=true
    - Implement `getValidAccessToken()` — check expiry, refresh if needed, handle `invalid_grant` by deleting tokens and throwing `GmailNotConnectedException`
    - Handle token exchange failures: return error, do not persist partial tokens
    - _Requirements: 1.2, 1.3, 1.4, 1.5, 1.6_

  - [x] 7.3 Add disconnect logic to GmailOAuthService
    - Implement `disconnect()` — call Google revocation endpoint, delete GmailToken row
    - Always delete locally even if revocation fails; return warning if revocation failed
    - _Requirements: 2.1, 2.2, 2.4, 2.5_

- [x] 8. Implement EmailSyncService
  - [x] 8.1 Create EmailSyncService with Gmail query construction
    - Create `com.project.kiro.service.EmailSyncService` as a `@Service`
    - Build Gmail search query filtering by known Indian bank sender addresses
    - Apply `after:<unix_timestamp>` filter based on last sync timestamp
    - If no previous sync, use 30-day lookback window
    - Fetch only body and headers (no attachments)
    - _Requirements: 13.1, 13.2, 13.3, 13.4_

  - [x] 8.2 Implement sync cycle orchestration in EmailSyncService
    - Implement `executeSyncCycle()` with AtomicBoolean sync lock
    - For each message: check dedup → fetch content → parse → skip credits → match category → create expense
    - Save ProcessedEmail records for all outcomes (EXPENSE_CREATED, CREDIT_SKIPPED, UNPARSEABLE)
    - Save expense + ProcessedEmail in single transaction per batch item
    - Rollback on DB failure, log error, continue to next email
    - Update SyncStatus on completion
    - _Requirements: 7.1, 7.2, 7.3, 7.5, 7.6, 7.7, 4.3, 8.3, 8.6_

  - [x] 8.3 Add concurrency control and timeout to EmailSyncService
    - Implement `isSyncInProgress()` using AtomicBoolean
    - Throw `SyncInProgressException` if sync already running
    - Implement 120-second timeout: abort, clear lock, update SyncStatus with FAILED, throw `SyncTimeoutException`
    - Clear in-progress flag on both success and failure
    - _Requirements: 9.5, 9.6, 9.7_

- [x] 9. Implement SyncScheduler
  - [x] 9.1 Create SyncScheduler configuration
    - Create `com.project.kiro.config.SyncScheduler` with `@EnableScheduling`
    - Use `@Scheduled(fixedDelayString)` with 15-minute interval from properties
    - Check `isConnected()` before triggering — skip silently if not connected
    - Catch and log all exceptions (do not propagate)
    - _Requirements: 8.1, 8.2, 8.4, 8.5_

- [x] 10. Implement EmailController
  - [x] 10.1 Create EmailController with OAuth endpoints
    - Create `com.project.kiro.controller.EmailController` with `@RestController` and `@RequestMapping("/api/v1/email")`
    - Implement `GET /auth` → returns `AuthUrlResponse`
    - Implement `GET /callback?code=...` → exchanges code, redirects to frontend with success/error query param
    - Implement `GET /status` → returns `ConnectionStatusResponse`
    - Implement `POST /disconnect` → returns `DisconnectResponse`
    - _Requirements: 1.1, 1.2, 1.5, 1.7, 1.8, 2.1, 2.4_

  - [x] 10.2 Add sync endpoints to EmailController
    - Implement `POST /sync` → triggers manual sync, returns `SyncResponse`
    - Implement `GET /sync/status` → returns `SyncStatusResponse`
    - Handle GmailNotConnectedException (400), SyncInProgressException (409), GmailApiException (502), SyncTimeoutException (504)
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.7, 11.1, 11.2_

- [x] 11. Checkpoint - Ensure backend compiles and tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 12. Implement frontend API module and GmailPanel
  - [x] 12.1 Create email.js API module
    - Create `frontend/src/api/email.js` with functions: `getConnectionStatus`, `getAuthUrl`, `disconnect`, `triggerSync`, `getSyncStatus`
    - Use existing `client.js` axios instance
    - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.7_

  - [x] 12.2 Create GmailPanel component
    - Create `frontend/src/components/gmail/GmailPanel.jsx`
    - Implement state machine: LOADING → DISCONNECTED/CONNECTED, CONNECTED → SYNCING
    - Disconnected state: show "Connect Gmail" button that redirects to OAuth auth URL
    - Connected state: show "Sync from Gmail" button, "Disconnect" button, last sync info
    - Fetch connection status and sync status on mount
    - _Requirements: 12.1, 12.2, 12.3, 12.7, 12.8_

  - [x] 12.3 Add sync and disconnect interactions to GmailPanel
    - "Sync from Gmail" click: call `triggerSync()`, disable button during request, show toast with importedCount on success
    - Show error toast on sync failure, re-enable button
    - "Disconnect" click: call `disconnect()`, update panel to disconnected state on success
    - Show error toast on disconnect failure, do not change state
    - _Requirements: 12.4, 12.5, 12.6, 12.9, 12.10_

  - [x] 12.4 Integrate GmailPanel into ExpensesPage
    - Import and render `GmailPanel` at the top of the Expenses page
    - Ensure panel is visible at all times on the Expenses page
    - _Requirements: 12.1_

- [x] 13. Write integration tests
  - [x]* 13.1 Write integration test for OAuth flow
    - Test full OAuth connect flow with mocked Google token endpoint (MockRestServiceServer)
    - Test disconnect flow (revocation success and failure)
    - Test token refresh and invalid_grant handling
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 2.1, 2.4_

  - [x]* 13.2 Write integration test for sync cycle
    - Test end-to-end sync with mocked Gmail API responses
    - Verify debit emails create expenses, credits are skipped, unparseable are logged
    - Verify deduplication: run sync twice with same messages → no duplicate expenses
    - Test sync status updates correctly
    - _Requirements: 7.1, 7.2, 7.3, 8.3, 8.6, 9.1, 9.2, 11.4_

  - [x]* 13.3 Write integration test for EmailController endpoints
    - Test `POST /sync` returns 400 when not connected
    - Test `POST /sync` returns 409 when sync in progress
    - Test `GET /sync/status` returns null timestamp when no sync completed
    - Test `GET /status` returns connected/disconnected correctly
    - _Requirements: 9.3, 9.5, 11.1, 11.2, 1.8_

- [x] 14. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design (EmailParser and CategoryMatcher)
- Unit tests validate specific examples and edge cases
- Google API interactions should be mocked in tests using MockRestServiceServer or similar
- The `DataInitializer` should be updated to seed the "Other" category if not already present

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2"] },
    { "id": 1, "tasks": ["2.1", "2.2", "2.3", "3.1", "3.2"] },
    { "id": 2, "tasks": ["2.4", "2.5", "2.6"] },
    { "id": 3, "tasks": ["5.1", "6.1"] },
    { "id": 4, "tasks": ["5.2", "5.3", "5.4", "5.5", "6.2"] },
    { "id": 5, "tasks": ["5.6", "7.1"] },
    { "id": 6, "tasks": ["7.2", "7.3"] },
    { "id": 7, "tasks": ["8.1"] },
    { "id": 8, "tasks": ["8.2", "8.3"] },
    { "id": 9, "tasks": ["9.1", "10.1"] },
    { "id": 10, "tasks": ["10.2"] },
    { "id": 11, "tasks": ["12.1"] },
    { "id": 12, "tasks": ["12.2"] },
    { "id": 13, "tasks": ["12.3", "12.4"] },
    { "id": 14, "tasks": ["13.1", "13.2", "13.3"] }
  ]
}
```
