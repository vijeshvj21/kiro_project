# Requirements Document

## Introduction

This document defines the requirements for the **Gmail Email Import** feature added to the existing Expense Tracker application (Spring Boot backend, `com.project.kiro` package; React frontend). The feature enables the application to automatically discover and import expense data from Indian bank debit notification emails received in a user's Gmail inbox. Users connect their Gmail account via Google OAuth2, after which the system polls Gmail in the background every 15 minutes and silently saves debit transactions as expenses. Users can also trigger an immediate sync on demand. Credit transactions are ignored. Imported expenses are deduplicated and auto-categorised based on the merchant name.

---

## Glossary

- **Gmail_OAuth_Service**: The backend Spring Boot service responsible for initiating and completing the Google OAuth2 authorization flow, storing access and refresh tokens, and revoking access on disconnect.
- **Email_Parser**: The backend component responsible for extracting transaction data (amount, date, merchant, transaction type) from raw Indian bank notification email bodies.
- **Email_Sync_Service**: The backend Spring Boot service responsible for querying Gmail for bank notification emails and persisting debit transactions as expenses.
- **Category_Matcher**: The backend component responsible for mapping a parsed merchant name to an existing expense category, falling back to "Other" when no rule matches.
- **Sync_Scheduler**: The background Spring scheduler that invokes Email_Sync_Service automatically every 15 minutes.
- **Sync_Status**: A record storing the timestamp of the last completed sync and the count of expenses imported during that sync.
- **Gmail_Panel**: The React UI section on the Expenses page that shows the Gmail connection state, last sync status, and provides the "Sync from Gmail" button.
- **User**: The individual using the application to track personal expenses.
- **Bank_Email**: An email sent by an Indian bank (e.g., HDFC, ICICI, SBI, Axis, Kotak) to notify the user of a debit or credit transaction on their account.
- **Debit_Transaction**: A Bank_Email representing money leaving the user's account (a payment or withdrawal), treated as an expense.
- **Credit_Transaction**: A Bank_Email representing money entering the user's account, not treated as an expense.
- **Email_ID**: The unique Gmail message identifier assigned by Google to a single email message; used for deduplication.
- **INR_Amount**: A monetary amount denominated in Indian Rupees, expressed in any of the recognised formats: `Rs. 450.00`, `INR 1,200`, or `₹500`.

---

## Requirements

### Requirement 1: Gmail OAuth2 Authorization — Connect Account

**User Story:** As a user, I want to connect my Gmail account to the expense tracker via Google OAuth2, so that the application can read my bank notification emails and import expenses automatically.

#### Acceptance Criteria

1. WHEN a user initiates Gmail account connection, THE Gmail_OAuth_Service SHALL redirect the user to Google's OAuth2 authorization endpoint requesting read-only Gmail access scope (`https://www.googleapis.com/auth/gmail.readonly`), with parameters `access_type=offline` and `prompt=consent` to ensure a refresh token is always issued.
2. WHEN Google redirects the user back to the application with a valid authorization code, THE Gmail_OAuth_Service SHALL exchange the authorization code for an access token and a refresh token, and SHALL persist both tokens in the application's server-side database (not in browser storage or cookies) before returning a success response.
3. IF the stored access token has expired and a valid refresh token exists, THEN THE Gmail_OAuth_Service SHALL use the refresh token to obtain a new access token from Google before making any Gmail API request, and SHALL update the persisted token record with the new access token.
4. IF the refresh token has itself been revoked or expired (Google returns an `invalid_grant` error), THEN THE Gmail_OAuth_Service SHALL delete the stored tokens and SHALL return a 401 Unauthorized response indicating the user must reconnect their Gmail account.
5. IF the OAuth2 authorization flow fails or the user denies permission, THEN THE Gmail_OAuth_Service SHALL return a descriptive error response to the frontend and SHALL NOT persist any token.
6. IF Google returns an error during the token exchange, THEN THE Gmail_OAuth_Service SHALL return an error response indicating the connection could not be completed, and SHALL NOT save a partial token.
7. WHEN the OAuth2 flow completes successfully, THE Gmail_OAuth_Service SHALL persist a `connected = true` flag alongside the tokens so that `GET /api/v1/email/status` can return the "connected" state on subsequent page loads without requiring a new OAuth flow.
8. WHEN a user requests the Gmail connection status and no token is stored, THE Gmail_OAuth_Service SHALL return a `{ "connected": false }` response with HTTP 200 OK.

---

### Requirement 2: Gmail OAuth2 Authorization — Disconnect Account

**User Story:** As a user, I want to disconnect my Gmail account from the expense tracker, so that the application stops reading my emails and I can revoke access at any time.

#### Acceptance Criteria

1. WHEN a user requests Gmail account disconnection, THE Gmail_OAuth_Service SHALL revoke the stored access token by calling Google's token revocation endpoint.
2. WHEN a user requests Gmail account disconnection, THE Gmail_OAuth_Service SHALL delete the stored access token and refresh token from persistent storage.
3. AFTER disconnection, THE Sync_Scheduler SHALL NOT attempt to poll Gmail on behalf of that user until a new OAuth2 connection is established.
4. WHEN a user requests Gmail account disconnection and the token revocation call fails, THE Gmail_OAuth_Service SHALL still delete the locally stored tokens and SHALL return a warning to the frontend indicating that revocation could not be confirmed with Google.
5. WHEN a user requests the Gmail connection status and no token is stored, THE Gmail_OAuth_Service SHALL return a "disconnected" status without error.

---

### Requirement 3: Bank Email Parsing — Amount Extraction

**User Story:** As a developer, I want the email parser to reliably extract transaction amounts from Indian bank notification emails, so that imported expenses have correct monetary values.

#### Acceptance Criteria

1. WHEN the Email_Parser processes a Bank_Email body containing an amount in the format `Rs. <amount>` (e.g., `Rs. 450.00`), THE Email_Parser SHALL extract the numeric value and store it as a `BigDecimal` with exactly 2 decimal places, using `HALF_UP` rounding if needed.
2. WHEN the Email_Parser processes a Bank_Email body containing an amount in the format `INR <amount>` (e.g., `INR 1,200.50`), THE Email_Parser SHALL remove all comma digit-group separators and extract the numeric value as a `BigDecimal` with exactly 2 decimal places.
3. WHEN the Email_Parser processes a Bank_Email body containing an amount in the format `₹<amount>` (e.g., `₹500.00`), THE Email_Parser SHALL extract the numeric value as a `BigDecimal` with exactly 2 decimal places.
4. IF the Email_Parser cannot find a recognisable INR_Amount in the email body, THEN THE Email_Parser SHALL mark the email as unparseable and SHALL NOT attempt to create an expense from it.
5. WHEN the Email_Parser processes any supported amount format containing comma digit-group separators (e.g., `1,20,000.00`), THE Email_Parser SHALL remove all commas before converting to a numeric value, yielding `120000.00`.
6. THE Email_Parser SHALL represent all extracted INR amounts as a `BigDecimal` value that, when converted to a two-decimal-place string, produces the same digits as the original amount stripped of currency prefix and commas.
7. WHEN the Email_Parser encounters a Bank_Email body containing more than one candidate INR_Amount, THE Email_Parser SHALL select the first matching amount found in document order and SHALL log a warning that multiple amounts were detected.

---

### Requirement 4: Bank Email Parsing — Transaction Type Detection

**User Story:** As a developer, I want the email parser to correctly identify debit vs. credit transactions, so that only debit transactions are imported as expenses.

#### Acceptance Criteria

1. WHEN the Email_Parser processes a Bank_Email body containing a debit indicator (e.g., keywords such as "debited", "debit", "withdrawn", "paid"), THE Email_Parser SHALL classify the transaction as a Debit_Transaction.
2. WHEN the Email_Parser processes a Bank_Email body containing a credit indicator (e.g., keywords such as "credited", "credit", "received"), THE Email_Parser SHALL classify the transaction as a Credit_Transaction.
3. WHEN the Email_Parser classifies a transaction as a Credit_Transaction, THE Email_Sync_Service SHALL skip the email and SHALL NOT create an expense record.
4. IF the Email_Parser cannot determine the transaction type from the email body, THEN THE Email_Parser SHALL mark the email as unparseable and THE Email_Sync_Service SHALL skip it without creating an expense.
5. THE Email_Parser SHALL apply transaction type detection case-insensitively to the full email body text.

---

### Requirement 5: Bank Email Parsing — Date Extraction

**User Story:** As a developer, I want the email parser to extract the transaction date from bank notification emails, so that imported expenses are recorded with the correct date.

#### Acceptance Criteria

1. WHEN the Email_Parser processes a Bank_Email body containing a transaction date in formats common to Indian bank emails (e.g., `DD-MM-YYYY`, `DD/MM/YYYY`, `DD MMM YYYY`), THE Email_Parser SHALL extract and use that date as the expense date.
2. IF the Email_Parser cannot find a transaction date in the email body, THEN THE Email_Parser SHALL use the date on which the email was received in Gmail as the expense date.
3. THE Email_Parser SHALL convert all extracted dates to the `LocalDate` type in the application's configured timezone before creating the expense record.
4. IF the extracted or fallback date is a future date relative to the current calendar day, THEN THE Email_Parser SHALL use the current calendar day as the expense date.

---

### Requirement 6: Bank Email Parsing — Merchant/Description Extraction

**User Story:** As a developer, I want the email parser to extract the merchant or transaction description from bank notification emails, so that imported expenses have meaningful descriptions.

#### Acceptance Criteria

1. WHEN the Email_Parser processes a Bank_Email body and identifies a merchant name or transaction reference (e.g., `SWIGGY`, `AMAZON`, `ATM withdrawal`), THE Email_Parser SHALL set the extracted value as the expense description, truncated to 255 characters.
2. IF the Email_Parser cannot identify a merchant or description in the email body, THEN THE Email_Parser SHALL use the email subject line as the expense description, truncated to 255 characters.
3. IF both the email body merchant extraction and the email subject line are empty or absent, THEN THE Email_Parser SHALL use the string `"Bank Transaction"` as the expense description.
4. THE Email_Parser SHALL normalise extracted merchant names to title case before storing them as the expense description.

---

### Requirement 7: Duplicate Detection

**User Story:** As a user, I want the system to avoid importing the same email twice, so that I do not end up with duplicate expense entries.

#### Acceptance Criteria

1. THE Email_Sync_Service SHALL store the Gmail Email_ID of every email it processes (regardless of whether an expense was created, the email was skipped as a credit, or the email was unparseable) so that the same email is never re-processed.
2. WHEN the Email_Sync_Service processes a batch of Bank_Emails, THE Email_Sync_Service SHALL check each email's Email_ID against the stored set of processed Email_IDs before creating an expense.
3. IF an incoming Bank_Email has an Email_ID that is already in the stored set, THEN THE Email_Sync_Service SHALL skip that email and SHALL NOT create a duplicate expense.
4. THE Email_Sync_Service SHALL persist the set of processed Email_IDs in the application's database so that deduplication survives application restarts.
5. WHEN the Email_Sync_Service completes processing a batch, THE Email_Sync_Service SHALL persist all newly encountered Email_IDs (including those skipped as credit or unparseable) in a single database transaction before updating the Sync_Status.
6. IF persisting the processed Email_IDs to the database fails, THEN THE Email_Sync_Service SHALL roll back the corresponding expense creation(s) for that batch item and SHALL log the error, so that the email can be safely re-processed on the next sync without producing orphaned records.
7. IF the deduplication check query itself fails with a database error, THEN THE Email_Sync_Service SHALL abort processing of the current batch item, log the error, and continue to the next email rather than proceeding without deduplication.

---

### Requirement 8: Automatic Background Sync

**User Story:** As a user, I want the application to automatically check my Gmail inbox every 15 minutes for new bank notification emails, so that my expenses are kept up to date without manual intervention.

#### Acceptance Criteria

1. THE Sync_Scheduler SHALL invoke the Email_Sync_Service at a fixed interval of 15 minutes after the previous execution completes.
2. WHILE the user's Gmail account is connected (a valid token is stored), THE Sync_Scheduler SHALL execute the polling cycle at each scheduled interval.
3. WHEN the Sync_Scheduler triggers a sync cycle, THE Email_Sync_Service SHALL query Gmail for Bank_Emails received since the timestamp of the last completed sync, to avoid re-processing already-seen emails unnecessarily.
4. IF the user's Gmail account is not connected (no token stored), THEN THE Sync_Scheduler SHALL skip the polling cycle for that interval without logging an error.
5. IF a sync cycle fails due to a transient error (e.g., network timeout, Gmail API rate limit), THEN THE Email_Sync_Service SHALL log the error with the failure reason and SHALL resume at the next scheduled interval.
6. WHEN the Sync_Scheduler completes a sync cycle, THE Email_Sync_Service SHALL update the Sync_Status record with the completion timestamp and the count of expenses created in that cycle.

---

### Requirement 9: Manual On-Demand Sync

**User Story:** As a user, I want to trigger a Gmail sync immediately by clicking a button in the UI, so that I can import new expenses without waiting for the next scheduled sync.

#### Acceptance Criteria

1. WHEN a user sends a `POST` request to `/api/v1/email/sync` and the Gmail account is connected, THE Email_Sync_Service SHALL execute a full sync cycle for new Bank_Emails received since the last sync timestamp.
2. WHEN the manual sync completes successfully, THE Email_Sync_Service SHALL return an HTTP 200 OK response containing a JSON body with the field `importedCount` set to the number of new expenses created during that sync cycle (zero is a valid value).
3. IF the user's Gmail account is not connected when `POST /api/v1/email/sync` is called, THEN THE Email_Sync_Service SHALL return a 400 Bad Request response with the message `"No Gmail account connected. Please connect your Gmail account first."`.
4. IF the manual sync fails due to a Gmail API error, THEN THE Email_Sync_Service SHALL return a 502 Bad Gateway response with a descriptive error message identifying the Gmail API failure.
5. WHEN a manual sync is already in progress and a second `POST /api/v1/email/sync` request arrives, THE Email_Sync_Service SHALL return a 409 Conflict response with the message `"A sync is already in progress."` The in-progress flag SHALL be cleared upon completion or failure of the running sync.
6. WHEN the manual sync completes (whether successfully or with a failure), THE Email_Sync_Service SHALL update the Sync_Status record with the completion timestamp and the count of expenses created (0 on failure).
7. IF the manual sync does not complete within 120 seconds, THE Email_Sync_Service SHALL abort the operation, clear the in-progress flag, update the Sync_Status with a failure status and the current timestamp, and return a 504 Gateway Timeout response.

---

### Requirement 10: Category Auto-Assignment

**User Story:** As a user, I want imported expenses to be automatically categorised based on the merchant name, so that my expense categories are populated without manual effort.

#### Acceptance Criteria

1. WHEN the Category_Matcher receives a merchant name containing any of the keywords `Swiggy` or `Zomato` (case-insensitive), THE Category_Matcher SHALL assign the category `Food` to the expense.
2. WHEN the Category_Matcher receives a merchant name containing any of the keywords `Uber`, `Ola`, `Rapido`, or `Metro` (case-insensitive), THE Category_Matcher SHALL assign the category `Transport` to the expense.
3. WHEN the Category_Matcher receives a merchant name containing any of the keywords `Amazon`, `Flipkart`, `Myntra`, or `Meesho` (case-insensitive), THE Category_Matcher SHALL assign the category `Shopping` to the expense.
4. WHEN the Category_Matcher receives a merchant name containing any of the keywords `Netflix`, `Spotify`, `Hotstar`, `Youtube`, or `Prime` (case-insensitive), THE Category_Matcher SHALL assign the category `Entertainment` to the expense.
5. WHEN the Category_Matcher receives a merchant name containing any of the keywords `Pharmacy`, `Hospital`, `Clinic`, `MedPlus`, or `Apollo` (case-insensitive), THE Category_Matcher SHALL assign the category `Healthcare` to the expense.
6. WHEN a merchant name matches keywords from more than one category rule (e.g., `"Amazon Prime"` matches both `Shopping` and `Entertainment`), THE Category_Matcher SHALL apply the first matching rule in declaration order (Food → Transport → Shopping → Entertainment → Healthcare) and SHALL NOT apply subsequent rules.
7. IF the Category_Matcher cannot match the merchant name to any known category keyword, THEN THE Category_Matcher SHALL assign the category `Other` to the expense.
8. WHEN the Category_Matcher receives a null or empty merchant name, THE Category_Matcher SHALL assign the category `Other` to the expense without attempting keyword matching.
9. WHEN the Category_Matcher assigns a category, THE Category_Matcher SHALL resolve the category entity by performing a case-insensitive name lookup against the database and SHALL NOT create a duplicate category record.
10. IF the resolved category name does not exist in the database, THEN THE Category_Matcher SHALL fall back to the `Other` category.

---

### Requirement 11: Sync Status Visibility

**User Story:** As a user, I want to see when the last Gmail sync ran and how many expenses were imported, so that I know the import feature is working and my data is current.

#### Acceptance Criteria

1. WHEN a user requests the sync status via `GET /api/v1/email/sync/status`, THE Email_Sync_Service SHALL return the timestamp of the last completed sync and the count of expenses created in that sync.
2. IF no sync has ever been completed, THEN THE Email_Sync_Service SHALL return a response with a null timestamp and a count of zero for the last sync status.
3. THE Email_Sync_Service SHALL persist the Sync_Status record in the database so that the status survives application restarts.
4. WHEN a sync completes (either scheduled or manual), THE Email_Sync_Service SHALL overwrite the previous Sync_Status record with the new timestamp and count.

---

### Requirement 12: Frontend — Gmail Panel on Expenses Page

**User Story:** As a user, I want a visible section on the Expenses page where I can connect my Gmail account, trigger a manual sync, and see the last sync status, so that I can manage email import without navigating to a separate settings page.

#### Acceptance Criteria

1. THE Gmail_Panel SHALL be displayed on the Expenses page at all times, showing either a "Connect Gmail" button when the account is disconnected or the sync controls when connected.
2. WHEN the Gmail account is disconnected, THE Gmail_Panel SHALL display a "Connect Gmail" button; WHEN clicked, THE Gmail_Panel SHALL initiate the Google OAuth2 authorization flow.
3. WHEN the Gmail account is connected, THE Gmail_Panel SHALL display a "Sync from Gmail" button and a "Disconnect" button.
4. WHEN a user clicks the "Sync from Gmail" button, THE Gmail_Panel SHALL call `POST /api/v1/email/sync` and SHALL disable the button for the duration of the request to prevent duplicate submissions.
5. WHEN the sync request completes successfully, THE Gmail_Panel SHALL display a toast notification showing the count of new expenses imported (e.g., "Imported 3 new expenses" or "No new expenses found").
6. IF the sync request fails, THEN THE Gmail_Panel SHALL display a toast notification with an error message and SHALL re-enable the "Sync from Gmail" button.
7. WHEN the Gmail account is connected, THE Gmail_Panel SHALL display the last sync timestamp and the count of expenses imported in the last sync, retrieved from `GET /api/v1/email/sync/status`.
8. IF no sync has been completed yet, THEN THE Gmail_Panel SHALL display "Never synced" in place of the last sync timestamp.
9. WHEN a user clicks the "Disconnect" button, THE Gmail_Panel SHALL call the disconnect endpoint and SHALL update the panel state to "disconnected" upon a successful response.
10. IF the disconnect request fails, THEN THE Gmail_Panel SHALL display an error toast and SHALL NOT change the displayed connection state.

---

### Requirement 13: Gmail API Query Filtering

**User Story:** As a developer, I want the system to query Gmail efficiently using targeted filters, so that the application only fetches relevant bank notification emails and minimises API quota usage.

#### Acceptance Criteria

1. WHEN the Email_Sync_Service queries Gmail for Bank_Emails, THE Email_Sync_Service SHALL apply a Gmail search query that restricts results to emails from known Indian bank sender domains or display names (e.g., HDFC Bank, ICICI Bank, SBI, Axis Bank, Kotak Mahindra Bank).
2. WHEN the Email_Sync_Service queries Gmail for Bank_Emails, THE Email_Sync_Service SHALL apply a `after:<unix_timestamp>` filter corresponding to the last sync timestamp so that only emails received after the previous sync are fetched.
3. IF no previous sync timestamp exists, THEN THE Email_Sync_Service SHALL query Gmail for Bank_Emails received within the past 30 days as the initial lookback window.
4. THE Email_Sync_Service SHALL retrieve only the email body and headers required for parsing (subject, sender, date, body text) and SHALL NOT download email attachments.

