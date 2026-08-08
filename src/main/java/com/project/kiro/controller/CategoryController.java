package com.project.kiro.controller;

import com.project.kiro.dto.request.CategoryRequest;
import com.project.kiro.dto.response.CategoryResponse;
import com.project.kiro.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing expense categories.
 * Satisfies Requirements 5.1–5.9, 5.11.
 */
@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * Retrieve all categories sorted alphabetically by name.
     * Satisfies Requirement 5.11.
     *
     * @return 200 OK with list of all categories
     */
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        List<CategoryResponse> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    /**
     * Create a new category.
     * Satisfies Requirements 5.1, 5.2, 5.3, 5.4, 5.5.
     *
     * @param request the category creation request (validated)
     * @return 201 Created with the created category
     */
    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse created = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update an existing category's name.
     * Satisfies Requirements 5.5, 5.6, 5.9.
     *
     * @param id      the ID of the category to update
     * @param request the update request containing the new name (validated)
     * @return 200 OK with the updated category
     */
    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        CategoryResponse updated = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete a category by ID.
     * Satisfies Requirements 5.7, 5.8.
     *
     * @param id the ID of the category to delete
     * @return 204 No Content on success
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
