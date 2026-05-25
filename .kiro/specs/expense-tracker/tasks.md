# Implementation Plan: Expense Tracker

## Overview

Implement a full-stack expense tracking application with a Java Spring Boot 4.0.6 backend (`com.project.kiro`) and a React JS frontend. The backend exposes a REST API backed by Spring Data JPA (H2 dev / PostgreSQL prod), with report generation via Apache Commons CSV and iText 7. The frontend uses React Router v6, Axios, Recharts, and Tailwind CSS. Property-based tests use jqwik (1.8.4).

---

## Tasks

- [x] 1. Project foundation — dependencies, configuration, and shared infrastructure
  - Add all required dependencies to `pom.xml`: `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `h2`, `postgresql`, `commons-csv`, `itext7-core`, `jqwik`
  - Configure `application.properties` with H2 datasource, JPA DDL auto, H2 console, and Jackson settings
  - Create `CorsConfig.java` in `com.project.kiro.config` to allow cross-origin requests from `http://localhost:3000`
  - Create `JacksonConfig.java` in `com.project.kiro.config` to configure `BigDecimal` serialization and ISO-8601 date formats
  - _Requirements: 1.1, 2.1, 5.1_

- [x] 2. Data models and JPA entities
  - [x] 2.1 Implement `Category` and `Expense` JPA entities
    - Create `Category.java` in `com.project.kiro.model` with `@Entity`, `@Table(uniqueConstraints)`, Lombok `@Data/@Builder`, `id`, `name`, `nameLower`, and `expenses` one-to-many
    - Create `Expense.java` in `com.project.kiro.model` with `@Entity`, `@Table(indexes)`, Lombok annotations, `id`, `amount` (`BigDecimal`), `expenseDate` (`LocalDate`), `category` (eager ManyToOne), `description`
    - _Requirements: 1.1, 1.8, 5.1_

  - [x] 2.2 Implement JPA repositories
    - Create `CategoryRepository.java` extending `JpaRepository<Category, Long>` with `findByNameLowerIgnoreCase` and `findAllByOrderByNameAsc`
    - Create `ExpenseRepository.java` extending `JpaRepository<Expense, Long>` with query methods for date-range filtering, weekly/monthly aggregation, and category breakdown (use `@Query` JPQL where needed)
    - _Requirements: 2.7, 3.1, 4.1, 4.5_

  - [ ]* 2.3 Write repository integration tests (`@DataJpaTest`)
    - Test weekly filter query returns only expenses in the requested ISO week
    - Test monthly filter query returns only expenses in the requested calendar month
    - Test category breakdown aggregation sums correctly per category
    - Test `DataInitializer` seeds the 7 default categories on startup
    - _Requirements: 3.1, 4.1, 4.5, 5.10_

- [ ] 3. Exception hierarchy and global error handler
  - [ ] 3.1 Create custom exception classes and `GlobalExceptionHandler`
    - Create `ResourceNotFoundException.java`, `CategoryInUseException.java`, `DuplicateCategoryException.java` in `com.project.kiro.exception`
    - Create `GlobalExceptionHandler.java` (`@RestControllerAdvice`) mapping each exception to the correct HTTP status and JSON error envelope (`timestamp`, `status`, `error`, `message`, `path`)
    - _Requirements: 1.2, 1.3, 1.4, 2.3, 2.6, 5.2, 5.3, 5.4, 5.8, 5.9_

  - [ ]* 3.2 Write unit tests for `GlobalExceptionHandler`
    - Verify each exception type maps to the correct HTTP status code
    - Verify the JSON error envelope contains all required fields
    - _Requirements: 1.2, 2.3, 5.2_

- [ ] 4. DTOs — request and response objects
  - Create `ExpenseRequest.java` with Bean Validation annotations (`@NotNull`, `@Positive`, `@DecimalMax`, `@PastOrPresent`, `@Size`)
  - Create `CategoryRequest.java` with `@NotBlank` and `@Size(max=100)`
  - Create `ReportRequest.java` with `startDate`, `endDate`, and `predefinedPeriod` fields
  - Create all response DTOs: `ExpenseResponse`, `WeeklyExpenseResponse`, `MonthlyExpenseResponse`, `CategoryResponse`, `ComparisonResponse`, `YearlyComparisonResponse`, `ReportResponse`
  - _Requirements: 1.1–1.9, 2.1, 3.1, 3.4, 4.1, 4.4, 4.5, 5.1, 9.1–9.7, 10.1–10.5, 11.1–11.4_

- [ ] 5. Category service and controller
  - [ ] 5.1 Implement `CategoryService`
    - Implement `createCategory`: trim name, set `nameLower`, check for duplicate (throw `DuplicateCategoryException` on conflict), persist, return `CategoryResponse`
    - Implement `updateCategory`: validate existence (throw `ResourceNotFoundException`), check duplicate name, update `name` and `nameLower`, persist
    - Implement `deleteCategory`: validate existence, check for associated expenses (throw `CategoryInUseException`), delete
    - Implement `getAllCategories`: return list sorted alphabetically by name
    - _Requirements: 5.1–5.9, 5.11_

  - [ ] 5.2 Implement `DataInitializer`
    - Create `DataInitializer.java` in `com.project.kiro.config` implementing `ApplicationRunner`
    - Seed the 7 default categories (Food, Transport, Utilities, Entertainment, Healthcare, Shopping, Other) if they do not already exist
    - _Requirements: 5.10_

  - [ ] 5.3 Implement `CategoryController`
    - Map `GET /api/v1/categories`, `POST /api/v1/categories`, `PUT /api/v1/categories/{id}`, `DELETE /api/v1/categories/{id}`
    - Use `@Valid` on request bodies; delegate to `CategoryService`
    - _Requirements: 5.1–5.9, 5.11_

  - [ ]* 5.4 Write property tests for `CategoryService` (jqwik)
    - **Property 17: Valid category names are always persisted and returned with a unique ID** — Validates: Requirements 5.1
    - **Property 18: Duplicate category names (case-insensitive) are always rejected with 409** — Validates: Requirements 5.2, 5.6
    - **Property 19: Whitespace-only category names are always rejected** — Validates: Requirements 5.3
    - **Property 20: Category names exceeding 100 characters are always rejected** — Validates: Requirements 5.4
    - **Property 21: Deleting a category with no expenses succeeds (round-trip)** — Validates: Requirements 5.7
    - **Property 22: Deleting a category with associated expenses is always rejected with 409** — Validates: Requirements 5.8
    - **Property 23: Category list is always sorted alphabetically by name** — Validates: Requirements 5.11

  - [ ]* 5.5 Write unit tests for `CategoryController` (MockMvc)
    - Test all 4 endpoints for happy-path and error responses
    - _Requirements: 5.1–5.9, 5.11_

- [ ] 6. Checkpoint — category layer
  - Ensure all category-related tests pass. Ask the user if questions arise.

- [ ] 7. Expense service and controller
  - [ ] 7.1 Implement `ExpenseService`
    - Implement `createExpense`: validate category exists, map request to entity, persist, return `ExpenseResponse` with HTTP 201
    - Implement `getExpenseById`: fetch by ID or throw `ResourceNotFoundException`
    - Implement `updateExpense`: validate existence and category, update fields, persist, return updated `ExpenseResponse`
    - Implement `deleteExpense`: validate existence, delete, return 204
    - Implement `getAllExpenses`: return sorted by `expenseDate` descending, capped at 1000 records
    - _Requirements: 1.1–1.9, 2.1–2.7_

  - [ ] 7.2 Implement `ExpenseController`
    - Map `POST /api/v1/expenses`, `GET /api/v1/expenses`, `GET /api/v1/expenses/{id}`, `PUT /api/v1/expenses/{id}`, `DELETE /api/v1/expenses/{id}`
    - Use `@Valid` on request bodies; delegate to `ExpenseService`
    - _Requirements: 1.1–1.9, 2.1–2.7_

  - [ ]* 7.3 Write property tests for expense creation and validation (jqwik, `@SpringBootTest`)
    - **Property 1: Valid expense creation always returns a unique ID** — Validates: Requirements 1.1
    - **Property 2: Non-positive amounts are always rejected** — Validates: Requirements 1.5
    - **Property 3: Out-of-range or over-precision amounts are always rejected** — Validates: Requirements 1.6
    - **Property 4: Future dates are always rejected** — Validates: Requirements 1.7
    - **Property 5: Descriptions within the 255-character limit are always accepted** — Validates: Requirements 1.8
    - **Property 6: Descriptions exceeding 255 characters are always rejected** — Validates: Requirements 1.9

  - [ ]* 7.4 Write property tests for expense management (jqwik)
    - **Property 7: Valid expense updates are persisted and returned correctly** — Validates: Requirements 2.1
    - **Property 8: Updates and deletes on non-existent IDs always return 404** — Validates: Requirements 2.3, 2.6
    - **Property 9: Delete then lookup returns 404 (round-trip deletion)** — Validates: Requirements 2.4
    - **Property 10: Expense list is always sorted by date descending** — Validates: Requirements 2.7

  - [ ]* 7.5 Write unit tests for `ExpenseController` (MockMvc)
    - Test null/missing field validation (requirements 1.2, 1.3, 1.4)
    - Test all CRUD endpoints for happy-path and error responses
    - _Requirements: 1.2, 1.3, 1.4, 2.1–2.7_

- [ ] 8. Weekly and monthly expense views
  - [ ] 8.1 Implement weekly view in `ExpenseService`
    - Add `getWeeklyExpenses(int year, int week)`: validate year (1900–2100) and ISO week number (throw `IllegalArgumentException` for invalid params), compute Monday–Sunday range, query repository, compute total, return `WeeklyExpenseResponse`
    - _Requirements: 3.1–3.4_

  - [ ] 8.2 Implement monthly view in `ExpenseService`
    - Add `getMonthlyExpenses(int year, int month)`: validate year and month (throw `IllegalArgumentException` for invalid params), compute first–last day of month, query repository, compute total and per-category breakdown, return `MonthlyExpenseResponse`
    - _Requirements: 4.1–4.5_

  - [ ] 8.3 Expose weekly and monthly endpoints in `ExpenseController`
    - Map `GET /api/v1/expenses/weekly?year={y}&week={w}` and `GET /api/v1/expenses/monthly?year={y}&month={m}`
    - _Requirements: 3.1–3.4, 4.1–4.5_

  - [ ]* 8.4 Write property tests for weekly view (jqwik)
    - **Property 11: Weekly view returns only expenses within the requested week, sorted descending** — Validates: Requirements 3.1
    - **Property 12: Invalid week parameters are always rejected** — Validates: Requirements 3.3
    - **Property 13: Weekly response total equals the sum of returned expense amounts** — Validates: Requirements 3.4

  - [ ]* 8.5 Write property tests for monthly view (jqwik)
    - **Property 14: Monthly view returns only expenses within the requested month, sorted descending** — Validates: Requirements 4.1
    - **Property 15: Invalid month parameters are always rejected** — Validates: Requirements 4.3
    - **Property 16: Monthly response total and per-category breakdown are mathematically consistent** — Validates: Requirements 4.4, 4.5

- [ ] 9. Checkpoint — expense CRUD and views
  - Ensure all expense service and view tests pass. Ask the user if questions arise.

- [ ] 10. Comparison service and endpoints
  - [ ] 10.1 Implement `ComparisonService`
    - Implement `compareMonths(int year, int month)`: compute specified and preceding month totals (0.00 if empty), absolute difference, percentage change (null + `percentageChangeAvailable=false` when preceding total is 0), and per-category breakdown with 0.00 for absent months; return `ComparisonResponse`
    - Implement `compareYears(int year)`: same logic at year granularity; per-month breakdown always has 12 entries (months 1–12) with 0.00 for empty months; return `YearlyComparisonResponse`
    - Use `HALF_UP` rounding to 2 decimal places for percentage change
    - _Requirements: 9.1–9.7, 10.1–10.5_

  - [ ] 10.2 Expose comparison endpoints in `ExpenseController`
    - Map `GET /api/v1/expenses/comparison/monthly?year={y}&month={m}` and `GET /api/v1/expenses/comparison/yearly?year={y}`
    - _Requirements: 9.1–9.7, 10.1–10.5_

  - [ ]* 10.3 Write property tests for month-over-month comparison (jqwik)
    - **Property 24: Month-over-month comparison totals are correct** — Validates: Requirements 9.1
    - **Property 25: Percentage change formula is correct when preceding period total is non-zero** — Validates: Requirements 9.3, 10.3
    - **Property 26: Month-over-month per-category breakdown is correct** — Validates: Requirements 9.5

  - [ ]* 10.4 Write property tests for year-over-year comparison (jqwik)
    - **Property 27: Directional indicator reflects the correct comparison direction** — Validates: Requirements 9.7
    - **Property 28: Year-over-year comparison totals are correct** — Validates: Requirements 10.1
    - **Property 29: Year-over-year per-month breakdown always has 12 entries with correct totals** — Validates: Requirements 10.5

  - [ ]* 10.5 Write unit tests for `ComparisonService`
    - Test percentage change formula with specific numeric examples (e.g., 0 preceding total, equal totals, increase, decrease)
    - _Requirements: 9.3, 9.4, 10.3, 10.4_

- [ ] 11. Report service and controller
  - [ ] 11.1 Implement `ReportService`
    - Implement predefined period resolution: `CURRENT_WEEK`, `CURRENT_MONTH`, `CURRENT_YEAR`, `LAST_MONTH`, `LAST_YEAR` → compute `startDate` and `endDate`
    - Implement `generateReport(ReportRequest)`: validate start ≤ end (throw `IllegalArgumentException` otherwise), resolve predefined period if set, fetch expenses in range, compute total, per-category totals, per-period (weekly and monthly) subtotals
    - _Requirements: 11.1–11.4, 11.7_

  - [ ] 11.2 Implement `CsvReportGenerator`
    - Use Apache Commons CSV to write header row (`ID,Date,Category,Amount,Description`), one row per expense (sorted by date descending), and a summary section (total, per-category totals, per-period subtotals)
    - Return `byte[]` for streaming as `text/csv`
    - _Requirements: 11.4, 11.5_

  - [ ] 11.3 Implement `PdfReportGenerator`
    - Use iText 7 Community to create a document with title and date-range header, summary table (total, per-category totals), per-period subtotal tables, and paginated expense list sorted by date descending
    - Return `byte[]` for streaming as `application/pdf`
    - _Requirements: 11.4, 11.6_

  - [ ] 11.4 Implement `ReportController`
    - Map `POST /api/v1/reports/csv` and `POST /api/v1/reports/pdf`
    - Stream `byte[]` response with correct `Content-Type` and `Content-Disposition: attachment; filename="report.csv"` / `"report.pdf"`
    - Return 400 for unsupported format paths
    - _Requirements: 11.1–11.8_

  - [ ]* 11.5 Write property tests for report service (jqwik)
    - **Property 30: Report date range filter is correct** — Validates: Requirements 11.1
    - **Property 31: Reports with start date after end date are always rejected** — Validates: Requirements 11.2
    - **Property 32: Report totals are mathematically consistent** — Validates: Requirements 11.4
    - **Property 33: CSV report structure is correct for any expense set** — Validates: Requirements 11.5
    - **Property 34: Unsupported report formats are always rejected** — Validates: Requirements 11.8

  - [ ]* 11.6 Write unit tests for `ReportService` predefined period resolution
    - Test all 5 predefined periods resolve to the correct `startDate` and `endDate`
    - _Requirements: 11.7_

  - [ ]* 11.7 Write unit tests for `CsvReportGenerator` and `PdfReportGenerator`
    - Test CSV output with a small fixed expense set: verify header, row count, and summary section
    - Test PDF generation does not throw and returns non-empty `byte[]`
    - _Requirements: 11.4, 11.5, 11.6_

- [ ] 12. Checkpoint — backend complete
  - Ensure all backend tests pass (unit, integration, property). Ask the user if questions arise.

- [ ] 13. React frontend — project setup and shared infrastructure
  - Initialise the React app (Vite) in a `frontend/` directory with React Router v6, Axios, Recharts, and Tailwind CSS
  - Create `src/api/client.js`: configure Axios base URL (`http://localhost:8080`), add response interceptor to dispatch non-2xx errors to a global error state
  - Create `src/api/expenses.js`, `src/api/categories.js`, `src/api/reports.js` with typed API functions for every backend endpoint
  - Create shared components: `ErrorMessage.jsx`, `LoadingSpinner.jsx`, `ConfirmDialog.jsx` in `src/components/common/`
  - Create `Layout.jsx` and `Navbar.jsx` in `src/components/layout/` with navigation links to all routes
  - Set up React Router v6 routes in `App.jsx`: `/`, `/expenses`, `/expenses/new`, `/expenses/:id/edit`, `/categories`, `/comparison`, `/reports`
  - _Requirements: 6.6, 7.1–7.6, 8.1–8.6_

- [ ] 14. Expense list and form pages
  - [ ] 14.1 Implement `ExpenseTable`, `ExpenseRow`, and `ExpenseListPage`
    - `ExpenseListPage` fetches `GET /api/v1/expenses` on mount and renders `ExpenseTable`
    - `ExpenseRow` shows amount, date, category, description with Edit and Delete buttons; Delete triggers `ConfirmDialog` before calling `DELETE /api/v1/expenses/{id}`
    - _Requirements: 2.4, 2.6, 2.7_

  - [ ] 14.2 Implement `ExpenseForm` and `ExpenseFormPage`
    - `ExpenseFormPage` at `/expenses/new` renders a blank form; at `/expenses/:id/edit` pre-fills from `GET /api/v1/expenses/{id}`
    - `ExpenseForm` validates client-side (positive amount, non-future date, category required) and submits to `POST` or `PUT`; displays field-level validation errors from the API response
    - _Requirements: 1.1–1.9, 2.1, 2.2_

  - [ ]* 14.3 Write Vitest + React Testing Library tests for `ExpenseForm`
    - Test client-side validation prevents submission with invalid data
    - Test form pre-fills correctly in edit mode
    - _Requirements: 1.2, 1.3, 1.4, 2.2_

- [ ] 15. Category management page
  - [ ] 15.1 Implement `CategoryList`, `CategoryForm`, and `CategoryPage`
    - `CategoryPage` fetches `GET /api/v1/categories` and renders `CategoryList` with inline edit and delete per row
    - `CategoryForm` handles create and update; shows 409 conflict and 400 validation errors from the API
    - Delete triggers `ConfirmDialog`; shows 409 error if category is in use
    - _Requirements: 5.1–5.9, 5.11_

  - [ ]* 15.2 Write Vitest tests for `CategoryList` and `CategoryForm`
    - Test alphabetical rendering of categories
    - Test error display for duplicate name and in-use delete
    - _Requirements: 5.2, 5.8, 5.11_

- [ ] 16. Dashboard — weekly, monthly, and yearly summary cards
  - [ ] 16.1 Implement `WeekNavigator`, `MonthNavigator`, `YearNavigator`
    - Each navigator maintains selected period in local state and exposes prev/next controls
    - `WeekNavigator` computes ISO week arithmetic; `MonthNavigator` and `YearNavigator` handle month/year rollover
    - _Requirements: 6.3, 7.4, 8.4_

  - [ ] 16.2 Implement `WeeklySummaryCard`
    - Fetches `GET /api/v1/expenses/weekly` for the selected week; displays total, entry count, and per-category breakdown
    - Shows `LoadingSpinner` during fetch and `ErrorMessage` with retry on failure
    - _Requirements: 6.1–6.6_

  - [ ] 16.3 Implement `MonthlySummaryCard` with `DailyBarChart`
    - Fetches `GET /api/v1/expenses/monthly` for the selected month; displays total, `DailyBarChart` (Recharts `BarChart` of daily totals), and per-category breakdown via `CategoryBreakdownTable`
    - _Requirements: 7.1–7.6_

  - [ ] 16.4 Implement `YearlySummaryCard` with `MonthlyLineChart`
    - Fetches `GET /api/v1/expenses/monthly` for each month of the selected year (or a dedicated yearly endpoint if added); displays total, `MonthlyLineChart` (Recharts `LineChart` of 12 monthly totals), and top-3 categories
    - _Requirements: 8.1–8.6_

  - [ ] 16.5 Implement `DashboardPage`
    - Fetches weekly, monthly, and yearly summaries in parallel with `Promise.all` on initial load
    - Renders `WeeklySummaryCard`, `MonthlySummaryCard`, `YearlySummaryCard` side by side
    - _Requirements: 6.1–6.6, 7.1–7.6, 8.1–8.6_

  - [ ]* 16.6 Write Vitest tests for navigator components
    - Test `WeekNavigator` ISO week arithmetic (week rollover, year boundary)
    - Test `MonthNavigator` and `YearNavigator` period arithmetic
    - _Requirements: 6.3, 7.4, 8.4_

- [ ] 17. Comparison page
  - [ ] 17.1 Implement `DirectionalIndicator`
    - Renders ↑ green arrow (UP), ↓ red arrow (DOWN), or → grey dash (NEUTRAL) based on `specifiedTotal` vs `precedingTotal`
    - _Requirements: 9.7_

  - [ ] 17.2 Implement `MonthComparisonView`
    - Fetches `GET /api/v1/expenses/comparison/monthly` for the selected month; displays totals, absolute difference, percentage change (or "N/A" when `percentageChangeAvailable=false`), `DirectionalIndicator`, and per-category breakdown table
    - _Requirements: 9.1–9.7_

  - [ ] 17.3 Implement `YearComparisonView` with `SideBySideBarChart`
    - Fetches `GET /api/v1/expenses/comparison/yearly` for the selected year; displays totals, absolute difference, percentage change, and `SideBySideBarChart` (Recharts grouped `BarChart` of 12 months × 2 years)
    - _Requirements: 10.1–10.6_

  - [ ] 17.4 Implement `ComparisonPage`
    - Renders `MonthComparisonView` and `YearComparisonView` with their respective navigators
    - _Requirements: 9.1–9.7, 10.1–10.6_

  - [ ]* 17.5 Write Vitest tests for `DirectionalIndicator`
    - Test UP, DOWN, and NEUTRAL cases with example inputs
    - _Requirements: 9.7_

- [ ] 18. Report page
  - [ ] 18.1 Implement `ReportForm` and `ReportDownloadButton`
    - `ReportForm` allows selecting a predefined period or custom date range and a format (CSV / PDF)
    - `ReportDownloadButton` calls `POST /api/v1/reports/{format}` and triggers a browser file download using a Blob URL
    - Displays 400 error messages (invalid date range, unsupported format) inline
    - _Requirements: 11.1–11.8_

  - [ ] 18.2 Implement `ReportPage`
    - Renders `ReportForm` with `ReportDownloadButton`; shows `LoadingSpinner` during generation
    - _Requirements: 11.1–11.8_

  - [ ]* 18.3 Write Vitest tests for `ReportForm`
    - Test that start-after-end date validation prevents submission
    - Test predefined period selection populates correct date fields
    - _Requirements: 11.2, 11.7_

- [ ] 19. Final checkpoint — full stack integration
  - Ensure all backend and frontend tests pass. Verify CORS configuration allows the React dev server to reach the Spring Boot API. Ask the user if questions arise.

---

## Notes

- Tasks marked with `*` are optional and can be skipped for a faster MVP
- Each task references specific requirements for traceability
- Property tests use jqwik 1.8.4 with `@Property(tries = 100)` and tag comments in the format `// Feature: expense-tracker, Property N: <title>`
- Unit tests use JUnit 5 + Mockito; controller tests use MockMvc; repository tests use `@DataJpaTest`
- Frontend tests use Vitest + React Testing Library; Axios is mocked with msw
- Checkpoints ensure incremental validation at each major layer boundary


## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["2.1", "3.1", "4"] },
    { "id": 1, "tasks": ["2.2", "5.2"] },
    { "id": 2, "tasks": ["2.3", "3.2", "5.1"] },
    { "id": 3, "tasks": ["5.3", "5.4", "5.5", "7.1"] },
    { "id": 4, "tasks": ["7.2", "8.1", "8.2", "10.1"] },
    { "id": 5, "tasks": ["7.3", "7.4", "7.5", "8.3", "11.1"] },
    { "id": 6, "tasks": ["8.4", "8.5", "10.2", "11.2", "11.3"] },
    { "id": 7, "tasks": ["10.3", "10.4", "10.5", "11.4"] },
    { "id": 8, "tasks": ["11.5", "11.6", "11.7", "13"] },
    { "id": 9, "tasks": ["14.1", "15.1", "16.1"] },
    { "id": 10, "tasks": ["14.2", "15.2", "16.2", "16.3", "16.4", "17.1"] },
    { "id": 11, "tasks": ["14.3", "16.5", "17.2", "17.3", "18.1"] },
    { "id": 12, "tasks": ["16.6", "17.4", "17.5", "18.2"] },
    { "id": 13, "tasks": ["18.3"] }
  ]
}
```
