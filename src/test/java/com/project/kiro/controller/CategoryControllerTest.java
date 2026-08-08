package com.project.kiro.controller;

import com.project.kiro.dto.request.CategoryRequest;
import com.project.kiro.dto.response.CategoryResponse;
import com.project.kiro.exception.CategoryInUseException;
import com.project.kiro.exception.DuplicateCategoryException;
import com.project.kiro.exception.GlobalExceptionHandler;
import com.project.kiro.exception.ResourceNotFoundException;
import com.project.kiro.service.CategoryService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link CategoryController} using MockMvc standalone setup.
 * Covers Requirements 5.1–5.9, 5.11.
 */
@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private CategoryController categoryController;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(categoryController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    // -----------------------------------------------------------------------
    // GET /api/v1/categories
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/v1/categories → 200 OK with list of categories")
    void getAllCategories_returnsOkWithList() throws Exception {
        List<CategoryResponse> categories = List.of(
                CategoryResponse.builder().id(1L).name("Food").build(),
                CategoryResponse.builder().id(2L).name("Transport").build()
        );
        when(categoryService.getAllCategories()).thenReturn(categories);

        mockMvc.perform(get("/api/v1/categories")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Food"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("Transport"));
    }

    // -----------------------------------------------------------------------
    // POST /api/v1/categories
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/v1/categories with valid body → 201 Created with created category")
    void createCategory_validRequest_returnsCreated() throws Exception {
        CategoryResponse response = CategoryResponse.builder().id(1L).name("Food").build();
        when(categoryService.createCategory(any(CategoryRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Food\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Food"));
    }

    @Test
    @DisplayName("POST /api/v1/categories with blank name → 400 Bad Request")
    void createCategory_blankName_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /api/v1/categories with name > 100 chars → 400 Bad Request")
    void createCategory_nameTooLong_returnsBadRequest() throws Exception {
        String longName = "A".repeat(101);
        String json = "{\"name\":\"" + longName + "\"}";

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /api/v1/categories when service throws DuplicateCategoryException → 409 Conflict")
    void createCategory_duplicateName_returnsConflict() throws Exception {
        when(categoryService.createCategory(any(CategoryRequest.class)))
                .thenThrow(new DuplicateCategoryException("Category already exists: Food"));

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Food\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    // -----------------------------------------------------------------------
    // PUT /api/v1/categories/{id}
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("PUT /api/v1/categories/{id} with valid body → 200 OK with updated category")
    void updateCategory_validRequest_returnsOk() throws Exception {
        CategoryResponse response = CategoryResponse.builder().id(1L).name("Groceries").build();
        when(categoryService.updateCategory(eq(1L), any(CategoryRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Groceries\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Groceries"));
    }

    @Test
    @DisplayName("PUT /api/v1/categories/{id} when service throws ResourceNotFoundException → 404 Not Found")
    void updateCategory_notFound_returnsNotFound() throws Exception {
        when(categoryService.updateCategory(eq(99L), any(CategoryRequest.class)))
                .thenThrow(new ResourceNotFoundException("Category", 99L));

        mockMvc.perform(put("/api/v1/categories/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Groceries\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // -----------------------------------------------------------------------
    // DELETE /api/v1/categories/{id}
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("DELETE /api/v1/categories/{id} → 204 No Content")
    void deleteCategory_success_returnsNoContent() throws Exception {
        doNothing().when(categoryService).deleteCategory(1L);

        mockMvc.perform(delete("/api/v1/categories/1"))
                .andExpect(status().isNoContent());

        verify(categoryService).deleteCategory(1L);
    }

    @Test
    @DisplayName("DELETE /api/v1/categories/{id} when service throws ResourceNotFoundException → 404 Not Found")
    void deleteCategory_notFound_returnsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Category", 99L))
                .when(categoryService).deleteCategory(99L);

        mockMvc.perform(delete("/api/v1/categories/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("DELETE /api/v1/categories/{id} when service throws CategoryInUseException → 409 Conflict")
    void deleteCategory_categoryInUse_returnsConflict() throws Exception {
        doThrow(new CategoryInUseException(1L))
                .when(categoryService).deleteCategory(1L);

        mockMvc.perform(delete("/api/v1/categories/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }
}
