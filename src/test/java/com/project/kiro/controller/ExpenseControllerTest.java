package com.project.kiro.controller;

import com.project.kiro.dto.response.ExpenseResponse;
import com.project.kiro.exception.GlobalExceptionHandler;
import com.project.kiro.exception.ResourceNotFoundException;
import com.project.kiro.service.ExpenseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link ExpenseController} using MockMvc standalone setup.
 * Covers Requirements 1.2, 1.3, 1.4, 2.1–2.7.
 */
@ExtendWith(MockitoExtension.class)
class ExpenseControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ExpenseService expenseService;

    @InjectMocks
    private ExpenseController expenseController;

    private ExpenseResponse sampleResponse;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(expenseController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();

        sampleResponse = ExpenseResponse.builder()
                .id(1L)
                .amount(new BigDecimal("50.00"))
                .expenseDate(LocalDate.now())
                .categoryId(2L)
                .categoryName("Food")
                .description("Lunch")
                .build();
    }

    // -----------------------------------------------------------------------
    // POST /api/v1/expenses — happy path
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/v1/expenses with valid body → 201 Created")
    void createExpense_validRequest_returnsCreated() throws Exception {
        when(expenseService.createExpense(any())).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":50.00,\"expenseDate\":\"" + LocalDate.now() + "\",\"categoryId\":2,\"description\":\"Lunch\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.amount").value(50.00))
                .andExpect(jsonPath("$.categoryId").value(2));
    }

    // -----------------------------------------------------------------------
    // POST /api/v1/expenses — validation failures
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/v1/expenses with null amount → 400 Bad Request (Req 1.2)")
    void createExpense_nullAmount_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expenseDate\":\"" + LocalDate.now() + "\",\"categoryId\":2}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /api/v1/expenses with null date → 400 Bad Request (Req 1.3)")
    void createExpense_nullDate_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":50.00,\"categoryId\":2}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /api/v1/expenses with null categoryId → 400 Bad Request (Req 1.4)")
    void createExpense_nullCategoryId_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":50.00,\"expenseDate\":\"" + LocalDate.now() + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /api/v1/expenses with amount 0 → 400 Bad Request (Req 1.5)")
    void createExpense_zeroAmount_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":0,\"expenseDate\":\"" + LocalDate.now() + "\",\"categoryId\":2}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /api/v1/expenses with amount > 999999999.99 → 400 Bad Request (Req 1.6)")
    void createExpense_amountTooLarge_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":1000000000.00,\"expenseDate\":\"" + LocalDate.now() + "\",\"categoryId\":2}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /api/v1/expenses with future date → 400 Bad Request (Req 1.7)")
    void createExpense_futureDate_returnsBadRequest() throws Exception {
        String futureDate = LocalDate.now().plusDays(1).toString();

        mockMvc.perform(post("/api/v1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":50.00,\"expenseDate\":\"" + futureDate + "\",\"categoryId\":2}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /api/v1/expenses with description > 255 chars → 400 Bad Request (Req 1.9)")
    void createExpense_descriptionTooLong_returnsBadRequest() throws Exception {
        String longDescription = "A".repeat(256);

        mockMvc.perform(post("/api/v1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":50.00,\"expenseDate\":\"" + LocalDate.now() + "\",\"categoryId\":2,\"description\":\"" + longDescription + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // -----------------------------------------------------------------------
    // GET /api/v1/expenses
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/v1/expenses → 200 OK with list (Req 2.7)")
    void getAllExpenses_returnsOkWithList() throws Exception {
        List<ExpenseResponse> expenses = List.of(
                sampleResponse,
                ExpenseResponse.builder()
                        .id(2L)
                        .amount(new BigDecimal("120.50"))
                        .expenseDate(LocalDate.now().minusDays(1))
                        .categoryId(3L)
                        .categoryName("Transport")
                        .description("Taxi")
                        .build()
        );
        when(expenseService.getAllExpenses()).thenReturn(expenses);

        mockMvc.perform(get("/api/v1/expenses")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));
    }

    // -----------------------------------------------------------------------
    // GET /api/v1/expenses/{id}
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/v1/expenses/{id} → 200 OK with expense (Req 2.1)")
    void getExpenseById_found_returnsOk() throws Exception {
        when(expenseService.getExpenseById(1L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/expenses/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.amount").value(50.00))
                .andExpect(jsonPath("$.categoryId").value(2));
    }

    @Test
    @DisplayName("GET /api/v1/expenses/{id} not found → 404 Not Found (Req 2.3)")
    void getExpenseById_notFound_returnsNotFound() throws Exception {
        when(expenseService.getExpenseById(99L))
                .thenThrow(new ResourceNotFoundException("Expense", 99L));

        mockMvc.perform(get("/api/v1/expenses/99")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // -----------------------------------------------------------------------
    // PUT /api/v1/expenses/{id}
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("PUT /api/v1/expenses/{id} with valid body → 200 OK (Req 2.1)")
    void updateExpense_validRequest_returnsOk() throws Exception {
        ExpenseResponse updated = ExpenseResponse.builder()
                .id(1L)
                .amount(new BigDecimal("75.00"))
                .expenseDate(LocalDate.now())
                .categoryId(2L)
                .categoryName("Food")
                .description("Dinner")
                .build();
        when(expenseService.updateExpense(eq(1L), any())).thenReturn(updated);

        mockMvc.perform(put("/api/v1/expenses/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":75.00,\"expenseDate\":\"" + LocalDate.now() + "\",\"categoryId\":2,\"description\":\"Dinner\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.amount").value(75.00))
                .andExpect(jsonPath("$.description").value("Dinner"));
    }

    @Test
    @DisplayName("PUT /api/v1/expenses/{id} not found → 404 Not Found (Req 2.3)")
    void updateExpense_notFound_returnsNotFound() throws Exception {
        when(expenseService.updateExpense(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("Expense", 99L));

        mockMvc.perform(put("/api/v1/expenses/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":50.00,\"expenseDate\":\"" + LocalDate.now() + "\",\"categoryId\":2}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // -----------------------------------------------------------------------
    // DELETE /api/v1/expenses/{id}
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("DELETE /api/v1/expenses/{id} → 204 No Content (Req 2.4)")
    void deleteExpense_success_returnsNoContent() throws Exception {
        doNothing().when(expenseService).deleteExpense(1L);

        mockMvc.perform(delete("/api/v1/expenses/1"))
                .andExpect(status().isNoContent());

        verify(expenseService).deleteExpense(1L);
    }

    @Test
    @DisplayName("DELETE /api/v1/expenses/{id} not found → 404 Not Found (Req 2.6)")
    void deleteExpense_notFound_returnsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Expense", 99L))
                .when(expenseService).deleteExpense(99L);

        mockMvc.perform(delete("/api/v1/expenses/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
