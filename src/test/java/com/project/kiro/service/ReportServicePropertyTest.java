package com.project.kiro.service;

import tools.jackson.databind.ObjectMapper;
import com.project.kiro.KiroApplication;
import com.project.kiro.dto.request.CategoryRequest;
import com.project.kiro.dto.request.ExpenseRequest;
import com.project.kiro.dto.response.CategoryResponse;
import com.project.kiro.dto.response.ExpenseResponse;
import com.project.kiro.dto.response.ReportResponse;
import com.project.kiro.repository.CategoryRepository;
import com.project.kiro.repository.ExpenseRepository;
import net.jqwik.api.*;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Property-based tests for the report service using jqwik.
 *
 * Feature: expense-tracker
 * Properties 30–34: Report generation correctness.
 *
 * Architecture note: jqwik creates its own test instances and does not support
 * Spring's @Autowired injection. We work around this by starting the Spring
 * application context once via SpringApplication.run() in a static initializer,
 * then resolving beans from that context for use in @Property methods.
 */
class ReportServicePropertyTest {

    // -------------------------------------------------------------------------
    // Spring context — started once for the entire test class (separate H2 DB)
    // -------------------------------------------------------------------------

    private static final ConfigurableApplicationContext ctx;
    private static final MockMvc mockMvc;
    private static final ObjectMapper objectMapper;
    private static final CategoryRepository categoryRepository;
    private static final ExpenseRepository expenseRepository;

    static {
        ctx = SpringApplication.run(KiroApplication.class,
                "--spring.datasource.url=jdbc:h2:mem:reportproptest;DB_CLOSE_DELAY=-1",
                "--spring.datasource.driver-class-name=org.h2.Driver",
                "--spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                "--spring.jpa.hibernate.ddl-auto=create-drop",
                "--spring.h2.console.enabled=false",
                "--server.port=0");

        mockMvc = MockMvcBuilders.webAppContextSetup((WebApplicationContext) ctx).build();
        objectMapper = ctx.getBean(ObjectMapper.class);
        categoryRepository = ctx.getBean(CategoryRepository.class);
        expenseRepository = ctx.getBean(ExpenseRepository.class);
    }

    // -------------------------------------------------------------------------
    // Helpers — category creation
    // -------------------------------------------------------------------------

    private CategoryResponse createCategory() throws Exception {
        String name = "ReportTest-" + UUID.randomUUID().toString().substring(0, 8);
        MvcResult result = mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CategoryRequest(name))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), CategoryResponse.class);
    }

    // -------------------------------------------------------------------------
    // Helpers — expense creation via REST
    // -------------------------------------------------------------------------

    private ExpenseResponse createExpense(BigDecimal amount, LocalDate date, Long categoryId) throws Exception {
        ExpenseRequest req = ExpenseRequest.builder()
                .amount(amount)
                .expenseDate(date)
                .categoryId(categoryId)
                .description("report-prop-test")
                .build();
        MvcResult result = mockMvc.perform(post("/api/v1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), ExpenseResponse.class);
    }

    // -------------------------------------------------------------------------
    // Helpers — expense deletion
    // -------------------------------------------------------------------------

    private void deleteExpense(Long id) throws Exception {
        mockMvc.perform(delete("/api/v1/expenses/" + id))
                .andExpect(status().isNoContent());
    }

    // -------------------------------------------------------------------------
    // Helpers — CSV report generation
    // -------------------------------------------------------------------------

    private String generateCsvReport(LocalDate startDate, LocalDate endDate) throws Exception {
        String requestBody = String.format(
                "{\"startDate\":\"%s\",\"endDate\":\"%s\"}", startDate, endDate);
        MvcResult result = mockMvc.perform(post("/api/v1/reports/csv")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private int generateCsvReportStatus(LocalDate startDate, LocalDate endDate) throws Exception {
        String requestBody = String.format(
                "{\"startDate\":\"%s\",\"endDate\":\"%s\"}", startDate, endDate);
        MvcResult result = mockMvc.perform(post("/api/v1/reports/csv")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andReturn();
        return result.getResponse().getStatus();
    }

    private int generatePdfReportStatus(LocalDate startDate, LocalDate endDate) throws Exception {
        String requestBody = String.format(
                "{\"startDate\":\"%s\",\"endDate\":\"%s\"}", startDate, endDate);
        MvcResult result = mockMvc.perform(post("/api/v1/reports/pdf")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andReturn();
        return result.getResponse().getStatus();
    }

    // -------------------------------------------------------------------------
    // Helpers — JSON report (uses reportService directly via the JSON endpoint
    //           by hitting CSV and parsing the ReportResponse from reportService)
    //           We call GET /api/v1/reports/csv and fall back to a helper that
    //           exercises reportService via the controller to get ReportResponse.
    //           Since there is no JSON report endpoint, we parse the CSV to verify
    //           totals. For Property 32, we POST to /csv and then independently
    //           sum amounts.
    // -------------------------------------------------------------------------

    /**
     * Generate a JSON-like ReportResponse by calling the CSV endpoint and
     * also computing the expected total from expenses created in the test.
     * The actual ReportResponse object is returned by calling the service
     * indirectly through the CSV generator. For Property 32 we read the
     * summary row from the CSV.
     */
    private ReportResponse generateReportResponse(LocalDate startDate, LocalDate endDate) throws Exception {
        String requestBody = String.format(
                "{\"startDate\":\"%s\",\"endDate\":\"%s\"}", startDate, endDate);
        // There's no direct JSON report endpoint, so we call the internal service
        // via a helper that exercises the same ReportService. We do this by
        // injecting the bean directly from the context.
        ReportService reportService = ctx.getBean(ReportService.class);
        com.project.kiro.dto.request.ReportRequest req =
                com.project.kiro.dto.request.ReportRequest.builder()
                        .startDate(startDate)
                        .endDate(endDate)
                        .build();
        return reportService.generateReport(req);
    }

    // =========================================================================
    // Property 30: Report date range filter is correct
    // Validates: Requirements 11.1
    // =========================================================================

    // Feature: expense-tracker, Property 30: Report date range filter is correct
    @Property(tries = 15)
    void property30_reportDateRangeFilterIsCorrect(
            @ForAll("reportDateRangeAndExpenses") Object[] params) throws Exception {

        LocalDate startDate = (LocalDate) params[0];
        LocalDate endDate = (LocalDate) params[1];
        LocalDate outsideBefore = (LocalDate) params[2];
        LocalDate outsideAfter = (LocalDate) params[3];
        @SuppressWarnings("unchecked")
        List<BigDecimal> insideAmounts = (List<BigDecimal>) params[4];

        CategoryResponse cat = createCategory();
        Long categoryId = cat.getId();
        List<Long> createdIds = new ArrayList<>();

        try {
            // Create expenses inside the range
            List<Long> insideIds = new ArrayList<>();
            for (int i = 0; i < insideAmounts.size(); i++) {
                // Spread inside dates across the range
                long daysInRange = endDate.toEpochDay() - startDate.toEpochDay();
                int dayOffset = (int) (((long) i * daysInRange) / Math.max(1, insideAmounts.size() - 1));
                if (insideAmounts.size() == 1) dayOffset = 0;
                LocalDate insideDate = startDate.plusDays(Math.min(dayOffset, daysInRange));
                ExpenseResponse exp = createExpense(insideAmounts.get(i), insideDate, categoryId);
                createdIds.add(exp.getId());
                insideIds.add(exp.getId());
            }

            // Create one expense strictly before the range
            ExpenseResponse beforeExp = createExpense(new BigDecimal("99.99"), outsideBefore, categoryId);
            createdIds.add(beforeExp.getId());

            // Create one expense strictly after the range (only if outsideAfter <= today)
            Long afterExpId = null;
            if (!outsideAfter.isAfter(LocalDate.now())) {
                ExpenseResponse afterExp = createExpense(new BigDecimal("88.88"), outsideAfter, categoryId);
                createdIds.add(afterExp.getId());
                afterExpId = afterExp.getId();
            }

            // Generate report and assert all returned expenses are within [startDate, endDate]
            ReportResponse report = generateReportResponse(startDate, endDate);

            for (ExpenseResponse expense : report.getExpenses()) {
                assertThat(expense.getExpenseDate())
                        .as("Expense date %s must be within [%s, %s]",
                                expense.getExpenseDate(), startDate, endDate)
                        .isAfterOrEqualTo(startDate)
                        .isBeforeOrEqualTo(endDate);
            }

            // The before expense must not appear
            final Long beforeId = beforeExp.getId();
            boolean beforeFound = report.getExpenses().stream()
                    .anyMatch(e -> e.getId().equals(beforeId));
            assertThat(beforeFound)
                    .as("Expense before range (id=%d, date=%s) must not appear in report [%s, %s]",
                            beforeExp.getId(), outsideBefore, startDate, endDate)
                    .isFalse();

            // The after expense must not appear (if it was created)
            if (afterExpId != null) {
                final Long afterId = afterExpId;
                boolean afterFound = report.getExpenses().stream()
                        .anyMatch(e -> e.getId().equals(afterId));
                assertThat(afterFound)
                        .as("Expense after range (id=%d, date=%s) must not appear in report [%s, %s]",
                                afterId, outsideAfter, startDate, endDate)
                        .isFalse();
            }

            // All inside expenses must appear
            for (Long insideId : insideIds) {
                boolean found = report.getExpenses().stream()
                        .anyMatch(e -> e.getId().equals(insideId));
                assertThat(found)
                        .as("Expense inside range (id=%d) must appear in report [%s, %s]",
                                insideId, startDate, endDate)
                        .isTrue();
            }

        } finally {
            for (Long id : createdIds) {
                try { deleteExpense(id); } catch (Exception ignored) {}
            }
            try { categoryRepository.deleteById(categoryId); } catch (Exception ignored) {}
        }
    }

    @Provide
    Arbitrary<Object[]> reportDateRangeAndExpenses() {
        // Use years 2000–2018 to ensure all dates (including outsideAfter) are in the past
        Arbitrary<BigDecimal> amountArb = Arbitraries.integers().between(100, 99999)
                .map(cents -> new BigDecimal(cents).divide(new BigDecimal(100)));

        return Arbitraries.integers().between(2000, 2018)
                .flatMap(year ->
                        Arbitraries.integers().between(1, 11)
                                .flatMap(startMonth ->
                                        Arbitraries.integers().between(1, 28)
                                                .flatMap(startDay ->
                                                        Arbitraries.integers().between(1, 3)
                                                                .flatMap(monthsAhead ->
                                                                        amountArb.list().ofMinSize(1).ofMaxSize(3)
                                                                                .map(amounts -> {
                                                                                    LocalDate start = LocalDate.of(year, startMonth, startDay);
                                                                                    LocalDate end = start.plusMonths(monthsAhead);
                                                                                    // outsideBefore: 1-10 days before start
                                                                                    LocalDate before = start.minusDays(5);
                                                                                    // outsideAfter: 1-10 days after end (in past)
                                                                                    LocalDate after = end.plusDays(5);
                                                                                    return new Object[]{start, end, before, after, amounts};
                                                                                })))));
    }

    // =========================================================================
    // Property 31: Reports with start date after end date are always rejected
    // Validates: Requirements 11.2
    // =========================================================================

    // Feature: expense-tracker, Property 31: Reports with start date after end date are always rejected
    @Property(tries = 15)
    void property31_reportsWithStartDateAfterEndDateAreAlwaysRejected() throws Exception {
        // Fixed case: startDate 2020-12-01, endDate 2020-01-01 (start >> end)
        LocalDate startDate = LocalDate.of(2020, 12, 1);
        LocalDate endDate = LocalDate.of(2020, 1, 1);

        int csvStatus = generateCsvReportStatus(startDate, endDate);
        assertThat(csvStatus)
                .as("POST /api/v1/reports/csv with startDate=%s > endDate=%s must return 400",
                        startDate, endDate)
                .isEqualTo(400);

        int pdfStatus = generatePdfReportStatus(startDate, endDate);
        assertThat(pdfStatus)
                .as("POST /api/v1/reports/pdf with startDate=%s > endDate=%s must return 400",
                        startDate, endDate)
                .isEqualTo(400);
    }

    // =========================================================================
    // Property 32: Report totals are mathematically consistent
    // Validates: Requirements 11.4
    // =========================================================================

    // Feature: expense-tracker, Property 32: Report totals are mathematically consistent
    @Property(tries = 15)
    void property32_reportTotalsAreMathematicallyConsistent(
            @ForAll("expenseSetForReport") Object[] params) throws Exception {

        LocalDate startDate = (LocalDate) params[0];
        LocalDate endDate = (LocalDate) params[1];
        @SuppressWarnings("unchecked")
        List<BigDecimal> amounts = (List<BigDecimal>) params[2];

        CategoryResponse cat = createCategory();
        Long categoryId = cat.getId();
        List<Long> createdIds = new ArrayList<>();

        try {
            // Create known expenses inside the range
            for (int i = 0; i < amounts.size(); i++) {
                long daysInRange = endDate.toEpochDay() - startDate.toEpochDay();
                int dayOffset = amounts.size() > 1
                        ? (int) (((long) i * daysInRange) / (amounts.size() - 1))
                        : 0;
                LocalDate date = startDate.plusDays(Math.min(dayOffset, daysInRange));
                ExpenseResponse exp = createExpense(amounts.get(i), date, categoryId);
                createdIds.add(exp.getId());
            }

            ReportResponse report = generateReportResponse(startDate, endDate);

            // Assert: total == sum of all expense amounts in the report
            BigDecimal sumFromExpenses = report.getExpenses().stream()
                    .map(ExpenseResponse::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            assertThat(report.getTotal())
                    .as("report.total (%s) must equal sum of expense amounts (%s)",
                            report.getTotal(), sumFromExpenses)
                    .isEqualByComparingTo(sumFromExpenses);

            // Assert: sum of categoryTotals == overall total
            if (report.getCategoryTotals() != null && !report.getCategoryTotals().isEmpty()) {
                BigDecimal sumFromCategories = report.getCategoryTotals().stream()
                        .map(ReportResponse.CategoryTotal::getTotal)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                assertThat(sumFromCategories)
                        .as("sum of categoryTotals (%s) must equal report.total (%s)",
                                sumFromCategories, report.getTotal())
                        .isEqualByComparingTo(report.getTotal());
            }

        } finally {
            for (Long id : createdIds) {
                try { deleteExpense(id); } catch (Exception ignored) {}
            }
            try { categoryRepository.deleteById(categoryId); } catch (Exception ignored) {}
        }
    }

    @Provide
    Arbitrary<Object[]> expenseSetForReport() {
        Arbitrary<BigDecimal> amountArb = Arbitraries.integers().between(100, 99999)
                .map(cents -> new BigDecimal(cents).divide(new BigDecimal(100)));

        // Use years/months safely in the past with a 1–2 month date range
        return Arbitraries.integers().between(2001, 2018)
                .flatMap(year ->
                        Arbitraries.integers().between(1, 10)
                                .flatMap(startMonth ->
                                        amountArb.list().ofMinSize(1).ofMaxSize(4)
                                                .map(amounts -> {
                                                    LocalDate start = LocalDate.of(year, startMonth, 1);
                                                    LocalDate end = start.plusMonths(2).minusDays(1);
                                                    return new Object[]{start, end, amounts};
                                                })));
    }

    // =========================================================================
    // Property 33: CSV report structure is correct for any expense set
    // Validates: Requirements 11.5
    // =========================================================================

    // Feature: expense-tracker, Property 33: CSV report structure is correct for any expense set
    @Property(tries = 15)
    void property33_csvReportStructureIsCorrectForAnyExpenseSet(
            @ForAll("expenseCountForCsv") Object[] params) throws Exception {

        LocalDate startDate = (LocalDate) params[0];
        LocalDate endDate = (LocalDate) params[1];
        int expenseCount = (int) params[2];
        @SuppressWarnings("unchecked")
        List<BigDecimal> amounts = (List<BigDecimal>) params[3];

        CategoryResponse cat = createCategory();
        Long categoryId = cat.getId();
        List<Long> createdIds = new ArrayList<>();

        try {
            // Create exactly expenseCount expenses inside the range
            for (int i = 0; i < expenseCount; i++) {
                long daysInRange = endDate.toEpochDay() - startDate.toEpochDay();
                int dayOffset = expenseCount > 1
                        ? (int) (((long) i * daysInRange) / (expenseCount - 1))
                        : 0;
                LocalDate date = startDate.plusDays(Math.min(dayOffset, daysInRange));
                ExpenseResponse exp = createExpense(amounts.get(i), date, categoryId);
                createdIds.add(exp.getId());
            }

            // Generate CSV report
            String csv = generateCsvReport(startDate, endDate);

            // Split into lines, normalising CRLF
            String[] allLines = csv.replace("\r\n", "\n").split("\n", -1);

            // Count lines until the first empty/blank line after the header.
            // The header is line[0]. Data rows start at line[1].
            // The summary section is separated by an empty line.
            int dataRowCount = 0;
            for (int i = 1; i < allLines.length; i++) {
                if (allLines[i].isBlank()) {
                    break; // reached the empty separator before the summary section
                }
                dataRowCount++;
            }

            assertThat(dataRowCount)
                    .as("CSV must have exactly %d data rows (one per expense) before the summary section, but found %d",
                            expenseCount, dataRowCount)
                    .isEqualTo(expenseCount);

            // Verify header is present as the first line
            assertThat(allLines[0])
                    .as("First CSV line must be the header row")
                    .contains("ID")
                    .contains("Date")
                    .contains("Category")
                    .contains("Amount")
                    .contains("Description");

        } finally {
            for (Long id : createdIds) {
                try { deleteExpense(id); } catch (Exception ignored) {}
            }
            try { categoryRepository.deleteById(categoryId); } catch (Exception ignored) {}
        }
    }

    @Provide
    Arbitrary<Object[]> expenseCountForCsv() {
        Arbitrary<BigDecimal> amountArb = Arbitraries.integers().between(100, 50000)
                .map(cents -> new BigDecimal(cents).divide(new BigDecimal(100)));

        return Arbitraries.integers().between(1, 5)
                .flatMap(count ->
                        Arbitraries.integers().between(2001, 2018)
                                .flatMap(year ->
                                        Arbitraries.integers().between(1, 10)
                                                .flatMap(startMonth ->
                                                        amountArb.list().ofSize(count)
                                                                .map(amounts -> {
                                                                    LocalDate start = LocalDate.of(year, startMonth, 1);
                                                                    LocalDate end = start.plusMonths(2).minusDays(1);
                                                                    return new Object[]{start, end, count, amounts};
                                                                }))));
    }

    // =========================================================================
    // Property 34: Unsupported report formats are always rejected
    // Validates: Requirements 11.8
    // =========================================================================

    // Feature: expense-tracker, Property 34: Unsupported report formats are always rejected
    @Property(tries = 15)
    void property34_unsupportedReportFormatsAreAlwaysRejected() throws Exception {
        // POST to /api/v1/reports/xlsx — unsupported format, must return 400
        String requestBody = "{\"startDate\":\"2020-01-01\",\"endDate\":\"2020-12-31\"}";

        MvcResult xlsxResult = mockMvc.perform(post("/api/v1/reports/xlsx")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andReturn();

        assertThat(xlsxResult.getResponse().getStatus())
                .as("POST /api/v1/reports/xlsx (unsupported format) must return 400")
                .isEqualTo(400);

        // Also test other unsupported formats for breadth
        for (String unsupportedFormat : List.of("json", "xml", "txt", "docx")) {
            MvcResult result = mockMvc.perform(post("/api/v1/reports/" + unsupportedFormat)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andReturn();

            assertThat(result.getResponse().getStatus())
                    .as("POST /api/v1/reports/%s (unsupported format) must return 400", unsupportedFormat)
                    .isEqualTo(400);
        }
    }
}
