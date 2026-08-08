package com.project.kiro.service;

import com.project.kiro.dto.request.CategoryRequest;
import com.project.kiro.dto.response.CategoryResponse;
import com.project.kiro.exception.CategoryInUseException;
import com.project.kiro.exception.DuplicateCategoryException;
import com.project.kiro.exception.ResourceNotFoundException;
import com.project.kiro.model.Category;
import com.project.kiro.repository.CategoryRepository;
import com.project.kiro.repository.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing expense categories.
 * Satisfies Requirements 5.1–5.9, 5.11.
 */
@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;

    public CategoryService(CategoryRepository categoryRepository, ExpenseRepository expenseRepository) {
        this.categoryRepository = categoryRepository;
        this.expenseRepository = expenseRepository;
    }

    /**
     * Create a new category.
     * Trims the name, computes nameLower, checks for duplicates (case-insensitive),
     * persists, and returns the mapped response.
     * Satisfies Requirements 5.1, 5.2, 5.3, 5.4, 5.5.
     *
     * @param request the category creation request
     * @return the created category as a CategoryResponse
     * @throws DuplicateCategoryException if a category with the same name already exists (case-insensitive)
     */
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        String name = request.getName().trim();
        String nameLower = name.toLowerCase();

        Optional<Category> existing = categoryRepository.findByNameLower(nameLower);
        if (existing.isPresent()) {
            throw new DuplicateCategoryException("Category already exists: " + name);
        }

        Category category = Category.builder()
                .name(name)
                .nameLower(nameLower)
                .build();

        Category saved = categoryRepository.save(category);
        return toResponse(saved);
    }

    /**
     * Update an existing category's name.
     * Validates existence, checks for duplicate name (excluding the current category),
     * updates fields, persists, and returns the mapped response.
     * Satisfies Requirements 5.5, 5.6, 5.9.
     *
     * @param id      the ID of the category to update
     * @param request the update request containing the new name
     * @return the updated category as a CategoryResponse
     * @throws ResourceNotFoundException  if no category with the given ID exists
     * @throws DuplicateCategoryException if another category already has the same name (case-insensitive)
     */
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));

        String name = request.getName().trim();
        String nameLower = name.toLowerCase();

        Optional<Category> existing = categoryRepository.findByNameLower(nameLower);
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            throw new DuplicateCategoryException("Category already exists: " + name);
        }

        category.setName(name);
        category.setNameLower(nameLower);

        Category saved = categoryRepository.save(category);
        return toResponse(saved);
    }

    /**
     * Delete a category by ID.
     * Validates existence and checks that no expenses reference this category before deleting.
     * Satisfies Requirements 5.7, 5.8.
     *
     * @param id the ID of the category to delete
     * @throws ResourceNotFoundException if no category with the given ID exists
     * @throws CategoryInUseException    if one or more expenses are associated with this category
     */
    @Transactional
    public void deleteCategory(Long id) {
        categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));

        long expenseCount = expenseRepository.countByCategoryId(id);
        if (expenseCount > 0) {
            throw new CategoryInUseException(id);
        }

        categoryRepository.deleteById(id);
    }

    /**
     * Retrieve all categories sorted alphabetically by name.
     * Satisfies Requirement 5.11.
     *
     * @return list of all categories ordered by name ascending
     */
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAllByOrderByNameAsc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Map a Category entity to a CategoryResponse DTO.
     *
     * @param category the entity to map
     * @return the mapped response
     */
    private CategoryResponse toResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }
}
