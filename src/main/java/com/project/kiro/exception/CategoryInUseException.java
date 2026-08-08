package com.project.kiro.exception;

public class CategoryInUseException extends RuntimeException {

    public CategoryInUseException(String message) {
        super(message);
    }

    public CategoryInUseException(Long categoryId) {
        super("Category with id " + categoryId + " is in use and cannot be deleted");
    }
}
