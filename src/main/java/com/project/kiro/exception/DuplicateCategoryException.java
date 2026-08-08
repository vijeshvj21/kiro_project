package com.project.kiro.exception;

public class DuplicateCategoryException extends RuntimeException {

    public DuplicateCategoryException(String message) {
        super(message);
    }

    public DuplicateCategoryException(String name, boolean caseInsensitive) {
        super("Category already exists with name: " + name);
    }
}
