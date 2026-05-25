# Requirements Document

## Introduction

This document defines the requirements for an Expense Tracking Application built with a Java Spring Boot backend (package `com.project.kiro`) and a React JS frontend. The application allows users to record personal expenses, organize them by category and time period, visualize spending trends on a dashboard, compare expenses across time periods, and generate reports. The system supports weekly and monthly expense entry workflows and provides yearly aggregations for long-term analysis.

## Glossary

- **Expense**: A single financial transaction recorded by the user, consisting of an amount, date, category, and optional description.
- **Category**: A user-defined or system-provided label used to classify expenses (e.g., Food, Transport, Utilities, Entertainment).
- **Expense_Service**: The backend Spring Boot service responsible for creating, reading, updating, and deleting expense records.
- **Category_Service**: The backend Spring Boot service responsible for managing expense categories.
- **Dashboard**: The frontend React component that displays aggregated expense data across time periods.
- **Report_Service**: The backend Spring Boot service responsible for generating expense summary reports.
- **Period**: A defined time window — either a week (Monday–Sunday), a calendar month, or a calendar year.
- **Comparison**: A side-by-side view of expense totals for two different periods of the same type.
- **User**: The authenticated individual using the application to track personal expenses.

---

## Requirements

### Requirement 1: Expense Entry

**User Story:** As a user, I want to add individual expense entries with a date, amount, category, and description, so that I can maintain an accurate record of my spending.

#### Acceptance Criteria

1. WHEN a user submits a new expense with a valid amount, date, and category, THE Expense_Service SHALL persist the expense record and return the created expense with a unique identifier and HTTP 201 Created status.
2. IF a user submits an expense with a missing or null amount, THEN THE Expense_Service SHALL return a 400 Bad Request response with a descriptive validation error message identifying the missing field.
3. IF a user submits an expense with a missing or null date, THEN THE Expense_Service SHALL return a 400 Bad Request response with a descriptive validation error message identifying the missing field.
4. IF a user submits an expense with a missing or null category, THEN THE Expense_Service SHALL return a 400 Bad Request response with a descriptive validation error message identifying the missing field.
5. IF a user submits an expense with an amount less than or equal to zero, THEN THE Expense_Service SHALL return a 400 Bad Request response indicating the amount must be a positive value.
6. IF a user submits an expense with an amount greater than 999,999,999.99 or with more than 2 decimal places, THEN THE Expense_Service SHALL return a 400 Bad Request response with a descriptive validation error message.
7. IF a user submits an expense with a future date beyond the current calendar day in the server's configured timezone, THEN THE Expense_Service SHALL return a 400 Bad Request response indicating future dates are not permitted.
8. THE Expense_Service SHALL accept an optional free-text description field of up to 255 characters for each expense entry.
9. IF a user submits an expense description exceeding 255 characters, THEN THE Expense_Service SHALL return a 400 Bad Request response with a descriptive validation error message.

---

### Requirement 2: Expense Management (Edit and Delete)

**User Story:** As a user, I want to edit or delete existing expense entries, so that I can correct mistakes and keep my records accurate.

#### Acceptance Criteria

1. WHEN a user submits an update request for an existing expense with valid fields (positive amount ≤ 999,999,999.99 with up to 2 decimal places, non-future date in server timezone, non-null category, description ≤ 255 characters), THE Expense_Service SHALL update the expense record and return the updated expense with HTTP 200 OK.
2. IF a user submits an update request for an existing expense with invalid fields, THEN THE Expense_Service SHALL return a 400 Bad Request response with a descriptive validation error message identifying each invalid field.
3. WHEN a user submits an update request for a non-existent expense identifier, THE Expense_Service SHALL return a 404 Not Found response.
4. WHEN a user submits a delete request for an existing expense identifier and the deletion succeeds, THE Expense_Service SHALL remove the expense record and return a 204 No Content response.
5. IF a user submits a delete request for an existing expense identifier and the deletion fails due to a database constraint or concurrent modification, THEN THE Expense_Service SHALL return a 500 Internal Server Error response with a descriptive error message.
6. WHEN a user submits a delete request for a non-existent expense identifier, THE Expense_Service SHALL return a 404 Not Found response.
7. WHEN a user requests a list of all expenses, THE Expense_Service SHALL return the expenses sorted by date in descending order, capped at 1000 records per response.

---

### Requirement 3: Weekly Expense View

**User Story:** As a user, I want to view all my expenses for a specific week, so that I can track my spending on a weekly basis.

#### Acceptance Criteria

1. WHEN a user requests expenses for a specific week identified by a year (1900–2100) and ISO week number, THE Expense_Service SHALL return all expense records whose dates fall within that Monday-to-Sunday range, sorted by date in descending order.
2. WHEN a user requests expenses for a week that contains no records, THE Expense_Service SHALL return an empty list with a 200 OK response.
3. IF a user requests expenses for a week with an invalid week number (less than 1, greater than 53, or week 53 in an ISO short year that only has 52 weeks) or a year outside the range 1900–2100, THEN THE Expense_Service SHALL return a 400 Bad Request response with a descriptive error message.
4. WHEN a user requests expenses for a valid week, THE Expense_Service SHALL include the total sum of all expense amounts in the response payload; this total SHALL be 0.00 when the expense list is empty.

---

### Requirement 4: Monthly Expense View

**User Story:** As a user, I want to view all my expenses for a specific month, so that I can track my spending on a monthly basis.

#### Acceptance Criteria

1. WHEN a user requests expenses for a specific month identified by a year (1900–2100) and month number (1–12), THE Expense_Service SHALL return all expense records whose dates fall within that calendar month, sorted by date in descending order.
2. WHEN a user requests expenses for a month that contains no records, THE Expense_Service SHALL return an empty list with a 200 OK response.
3. IF a user requests expenses for an invalid month number (less than 1 or greater than 12) or a year outside the range 1900–2100, THEN THE Expense_Service SHALL return a 400 Bad Request response with a descriptive error message.
4. WHEN a user requests expenses for a valid month, THE Expense_Service SHALL include the total sum of all expense amounts in the response payload; this total SHALL be 0.00 when the expense list is empty.
5. WHEN a user requests expenses for a valid month, THE Expense_Service SHALL include a per-category breakdown in the response payload, where each entry contains the category name and the sum of expense amounts for that category within the month.

---

### Requirement 5: Category Management

**User Story:** As a user, I want to create, edit, and delete expense categories, so that I can organize my expenses in a way that reflects my personal spending habits.

#### Acceptance Criteria

1. WHEN a user creates a new category with a unique name (1–100 characters, case-insensitive uniqueness check), THE Category_Service SHALL persist the category and return the created category with a unique identifier and HTTP 201 Created status.
2. IF a user creates a category with a name that already exists (case-insensitive match), THEN THE Category_Service SHALL return a 409 Conflict response with a descriptive error message identifying the conflicting name.
3. IF a user creates a category with a blank, null, or whitespace-only name, THEN THE Category_Service SHALL return a 400 Bad Request response with a descriptive error message.
4. IF a user creates a category with a name exceeding 100 characters, THEN THE Category_Service SHALL return a 400 Bad Request response with a descriptive error message.
5. WHEN a user updates an existing category name to a value that is unique (case-insensitive) and between 1–100 characters, THE Category_Service SHALL update the category and return the updated record with HTTP 200 OK.
6. IF a user updates a category with a name that conflicts with an existing category or violates length constraints, THEN THE Category_Service SHALL return the appropriate 409 Conflict or 400 Bad Request response.
7. WHEN a user deletes a category that has no associated expenses, THE Category_Service SHALL remove the category and return a 204 No Content response.
8. IF a user deletes a category that has one or more associated expenses, THEN THE Category_Service SHALL return a 409 Conflict response indicating the category is in use and cannot be deleted.
9. IF a user deletes a category that does not exist, THEN THE Category_Service SHALL return a 404 Not Found response.
10. THE Category_Service SHALL provide a default set of categories (Food, Transport, Utilities, Entertainment, Healthcare, Shopping, Other) on initial system setup, and these default categories SHALL be present before any user-initiated category creation.
11. WHEN a user requests all categories, THE Category_Service SHALL return the full list of categories sorted alphabetically by name.

---

### Requirement 6: Dashboard — Weekly Summary

**User Story:** As a user, I want to see a weekly expense summary on the dashboard, so that I can quickly understand my spending for the current and past weeks.

#### Acceptance Criteria

1. WHEN a user loads the Dashboard or selects a week using the week navigator, THE Dashboard SHALL display the total expense amount for the selected ISO week.
2. WHEN a user loads the Dashboard or selects a week using the week navigator, THE Dashboard SHALL display a per-category breakdown for the selected week, where each entry shows the category name and its total expense amount.
3. WHEN a user selects a different week using the week navigator, THE Dashboard SHALL update the total expense amount, per-category breakdown, and entry count to reflect the selected week without a full page reload.
4. WHEN a user loads the Dashboard or selects a week using the week navigator, THE Dashboard SHALL display the number of individual expense entries recorded in the selected week.
5. WHEN a user loads the Dashboard or selects a week for which no expense records exist, THE Dashboard SHALL display a total of 0.00, an empty category breakdown, and an entry count of 0.
6. IF the Dashboard fails to retrieve weekly expense data from the backend, THEN THE Dashboard SHALL display an error message indicating data could not be loaded and provide a retry option.

---

### Requirement 7: Dashboard — Monthly Summary

**User Story:** As a user, I want to see a monthly expense summary on the dashboard, so that I can monitor my spending across a full calendar month.

#### Acceptance Criteria

1. WHEN a user loads the Dashboard or selects a month using the month navigator, THE Dashboard SHALL display the total expense amount for the selected calendar month.
2. WHEN a user loads the Dashboard or selects a month using the month navigator, THE Dashboard SHALL display a bar chart showing daily expense totals for each day in the selected month, with days that have no expenses shown as 0.
3. WHEN a user loads the Dashboard or selects a month using the month navigator, THE Dashboard SHALL display a per-category breakdown for the selected month, where each entry shows the category name and its total expense amount.
4. WHEN a user selects a different month using the month navigator, THE Dashboard SHALL update the total, bar chart, and category breakdown to reflect the selected month without a full page reload.
5. WHEN a user loads the Dashboard or selects a month for which no expense records exist, THE Dashboard SHALL display a total of 0.00, a bar chart with all days at 0, and an empty category breakdown.
6. THE Dashboard month navigator SHALL allow selection of any month from January 1900 through December 2100.

---

### Requirement 8: Dashboard — Yearly Summary

**User Story:** As a user, I want to see a yearly expense summary on the dashboard, so that I can understand my annual spending patterns.

#### Acceptance Criteria

1. WHEN a user loads the Dashboard or selects a year using the year navigator, THE Dashboard SHALL display the total expense amount for the selected calendar year.
2. WHEN a user loads the Dashboard or selects a year using the year navigator, THE Dashboard SHALL display a chart showing monthly expense totals for all 12 months of the selected year, with months that have no expenses shown as 0.
3. WHEN a user loads the Dashboard or selects a year using the year navigator, THE Dashboard SHALL display the top three spending categories by total amount for the selected year; if fewer than three categories have expenses, THE Dashboard SHALL display only the categories that have a non-zero total.
4. WHEN a user selects a different year using the year navigator, THE Dashboard SHALL update the total, monthly chart, and top categories to reflect the selected year without a full page reload.
5. WHEN a user loads the Dashboard or selects a year for which no expense records exist, THE Dashboard SHALL display a total of 0.00, a chart with all 12 months at 0, and an empty top-categories list.
6. THE Dashboard year navigator SHALL allow selection of any year in the range 1900–2100.

---

### Requirement 9: Expense Comparison — Month over Month

**User Story:** As a user, I want to compare my expenses for the current month against the previous month, so that I can identify whether my spending has increased or decreased.

#### Acceptance Criteria

1. WHEN a user requests a month-over-month comparison for a given month, THE Expense_Service SHALL return the total expense amounts for the specified month and the immediately preceding calendar month, returning 0.00 for any month with no expense records.
2. WHEN a user requests a month-over-month comparison for a given month, THE Expense_Service SHALL return the absolute difference (specified month total minus preceding month total) between the two monthly totals.
3. WHEN a user requests a month-over-month comparison for a given month and the preceding month total is greater than zero, THE Expense_Service SHALL return the percentage change calculated as ((specified month total − preceding month total) / preceding month total) × 100, rounded to two decimal places.
4. IF the preceding month total is zero, THEN THE Expense_Service SHALL return null for the percentage change field and include a boolean field `percentageChangeAvailable` set to false.
5. WHEN a user requests a month-over-month comparison, THE Expense_Service SHALL return a per-category breakdown for both months, where each category entry contains the category name, the total for the specified month, and the total for the preceding month; categories present in one month but absent in the other SHALL have a total of 0.00 for the absent month.
6. IF the specified month itself has no expense records, THEN THE Expense_Service SHALL return 0.00 for the specified month total, the preceding month total as-is, and set `percentageChangeAvailable` to false.
7. WHEN a user views the month-over-month comparison on the Dashboard, THE Dashboard SHALL display a directional indicator: an upward arrow when the specified month total is greater than the preceding month total, a downward arrow when it is less, and a neutral indicator when they are equal.

---

### Requirement 10: Expense Comparison — Year over Year

**User Story:** As a user, I want to compare my expenses for the current year against the previous year, so that I can evaluate my long-term spending trends.

#### Acceptance Criteria

1. WHEN a user requests a year-over-year comparison for a given year, THE Expense_Service SHALL return the total expense amounts for the specified year and the immediately preceding calendar year, returning 0.00 for any year with no expense records.
2. WHEN a user requests a year-over-year comparison for a given year, THE Expense_Service SHALL return the absolute difference (specified year total minus preceding year total) between the two yearly totals.
3. WHEN a user requests a year-over-year comparison for a given year and the preceding year total is greater than zero, THE Expense_Service SHALL return the percentage change calculated as ((specified year total − preceding year total) / preceding year total) × 100, rounded to two decimal places.
4. IF the preceding year total is zero, THEN THE Expense_Service SHALL return null for the percentage change field and include a boolean field `percentageChangeAvailable` set to false.
5. WHEN a user requests a year-over-year comparison, THE Expense_Service SHALL return a per-month breakdown for both years, where each entry contains the month number (1–12) and the total expense amount for that month; months with no expenses SHALL have a total of 0.00.
6. WHEN a user views the year-over-year comparison on the Dashboard, THE Dashboard SHALL display a side-by-side chart showing monthly totals for both years on a shared time axis (months 1–12).

---

### Requirement 11: Report Generation

**User Story:** As a user, I want to generate and download expense reports for a selected time period, so that I can review or share a detailed summary of my spending.

#### Acceptance Criteria

1. WHEN a user requests a report for a specified date range with a valid start date and end date (inclusive on both ends), THE Report_Service SHALL generate a report containing all expense records whose dates fall within that range.
2. IF a user requests a report where the start date is after the end date, THEN THE Report_Service SHALL return a 400 Bad Request response with a descriptive error message.
3. WHEN a user requests a report for a date range that contains no expense records, THE Report_Service SHALL return a report with a zero total and an empty expense list.
4. THE Report_Service SHALL include in each report: total expense amount, per-category totals, per-period (weekly and monthly) subtotals, and the full list of individual expense entries sorted by date in descending order.
5. WHEN a user requests a report in CSV format, THE Report_Service SHALL return a downloadable CSV file with a header row followed by one expense entry per row containing the same fields defined in criterion 4.
6. WHEN a user requests a report in PDF format, THE Report_Service SHALL return a downloadable PDF file containing the total expense amount, per-category totals, per-period subtotals, and the full expense entry list as defined in criterion 4.
7. WHEN a user selects a predefined period (current week, current month, current year, last month, or last year), THE Report_Service SHALL resolve the corresponding start and end dates and generate the report as defined in criterion 1.
8. IF a user requests a report in an unsupported format, THEN THE Report_Service SHALL return a 400 Bad Request response listing the supported formats (CSV and PDF).
