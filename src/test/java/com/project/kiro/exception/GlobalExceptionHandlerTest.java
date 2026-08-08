package com.project.kiro.exception;

import com.project.kiro.dto.request.CategoryRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link GlobalExceptionHandler}.
 *
 * Uses MockMvc standaloneSetup with a minimal inner test controller that
 * throws each exception type, so no Spring context or database is needed.
 *
 * Validates: Requirements 1.2, 2.3, 5.2
 */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    // -----------------------------------------------------------------------
    // Minimal inner controller that throws each exception type
    // -----------------------------------------------------------------------
    @RestController
    @RequestMapping("/test")
    static class TestController {

        @GetMapping("/resource-not-found")
        public void throwResourceNotFound() {
            throw new ResourceNotFoundException("Category", 99L);
        }

        @GetMapping("/duplicate-category")
        public void throwDuplicateCategory() {
            throw new DuplicateCategoryException("Food", true);
        }

        @GetMapping("/category-in-use")
        public void throwCategoryInUse() {
            throw new CategoryInUseException(5L);
        }

        @GetMapping("/illegal-argument")
        public void throwIllegalArgument() {
            throw new IllegalArgumentException("Invalid period parameter");
        }

        @PostMapping("/validate")
        public void validateBody(@RequestBody @jakarta.validation.Valid CategoryRequest request) {
            // If validation passes, do nothing — test sends an invalid body
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // -----------------------------------------------------------------------
    // 1. ResourceNotFoundException → 404 Not Found
    // -----------------------------------------------------------------------
    @Test
    void resourceNotFoundException_returns404WithCorrectEnvelope() throws Exception {
        mockMvc.perform(get("/test/resource-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.path").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    // -----------------------------------------------------------------------
    // 2. DuplicateCategoryException → 409 Conflict
    // -----------------------------------------------------------------------
    @Test
    void duplicateCategoryException_returns409WithCorrectEnvelope() throws Exception {
        mockMvc.perform(get("/test/duplicate-category"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.path").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    // -----------------------------------------------------------------------
    // 3. CategoryInUseException → 409 Conflict
    // -----------------------------------------------------------------------
    @Test
    void categoryInUseException_returns409WithCorrectEnvelope() throws Exception {
        mockMvc.perform(get("/test/category-in-use"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.path").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    // -----------------------------------------------------------------------
    // 4. IllegalArgumentException → 400 Bad Request
    // -----------------------------------------------------------------------
    @Test
    void illegalArgumentException_returns400WithCorrectEnvelope() throws Exception {
        mockMvc.perform(get("/test/illegal-argument"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.path").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    // -----------------------------------------------------------------------
    // 5. MethodArgumentNotValidException → 400 Bad Request
    //    Triggered by posting a CategoryRequest with a blank name (@NotBlank)
    // -----------------------------------------------------------------------
    @Test
    void methodArgumentNotValidException_returns400WithCorrectEnvelope() throws Exception {
        // CategoryRequest.name is @NotBlank — send blank to trigger validation failure
        String json = "{\"name\":\"\"}";

        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.path").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    // -----------------------------------------------------------------------
    // 6. Verify all five required envelope fields are present for each case
    // -----------------------------------------------------------------------
    @Test
    void allEnvelopeFieldsPresent_forResourceNotFoundException() throws Exception {
        mockMvc.perform(get("/test/resource-not-found"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").exists());
    }

    @Test
    void allEnvelopeFieldsPresent_forDuplicateCategoryException() throws Exception {
        mockMvc.perform(get("/test/duplicate-category"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").exists());
    }

    @Test
    void allEnvelopeFieldsPresent_forCategoryInUseException() throws Exception {
        mockMvc.perform(get("/test/category-in-use"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").exists());
    }

    @Test
    void allEnvelopeFieldsPresent_forIllegalArgumentException() throws Exception {
        mockMvc.perform(get("/test/illegal-argument"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").exists());
    }

    @Test
    void allEnvelopeFieldsPresent_forMethodArgumentNotValidException() throws Exception {
        String json = "{\"name\":\"\"}";

        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").exists());
    }

    // -----------------------------------------------------------------------
    // 7. Verify path field reflects the actual request URI
    // -----------------------------------------------------------------------
    @Test
    void pathField_reflectsActualRequestUri() throws Exception {
        mockMvc.perform(get("/test/resource-not-found"))
                .andExpect(jsonPath("$.path").value("/test/resource-not-found"));
    }

    // -----------------------------------------------------------------------
    // 8. Verify message field contains the exception message
    // -----------------------------------------------------------------------
    @Test
    void messageField_containsExceptionMessage_forResourceNotFoundException() throws Exception {
        mockMvc.perform(get("/test/resource-not-found"))
                // ResourceNotFoundException("Category", 99L) → "Category not found with id: 99"
                .andExpect(jsonPath("$.message").value("Category not found with id: 99"));
    }

    @Test
    void messageField_containsExceptionMessage_forIllegalArgumentException() throws Exception {
        mockMvc.perform(get("/test/illegal-argument"))
                .andExpect(jsonPath("$.message").value("Invalid period parameter"));
    }
}
