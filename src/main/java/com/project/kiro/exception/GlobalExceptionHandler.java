package com.project.kiro.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // -----------------------------------------------------------------------
    // 400 Bad Request — Bean Validation (@Valid on request body)
    // -----------------------------------------------------------------------
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        return buildResponse(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
    }

    // -----------------------------------------------------------------------
    // 400 Bad Request — Constraint Violation (@Validated on path/query params)
    // -----------------------------------------------------------------------
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {

        String message = ex.getConstraintViolations().stream()
                .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                .collect(Collectors.joining("; "));

        return buildResponse(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
    }

    // -----------------------------------------------------------------------
    // 400 Bad Request — Illegal argument (invalid period params, etc.)
    // -----------------------------------------------------------------------
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    // -----------------------------------------------------------------------
    // 404 Not Found — Resource not found
    // -----------------------------------------------------------------------
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {

        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }

    // -----------------------------------------------------------------------
    // 409 Conflict — Duplicate category name
    // -----------------------------------------------------------------------
    @ExceptionHandler(DuplicateCategoryException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateCategory(
            DuplicateCategoryException ex,
            HttpServletRequest request) {

        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI());
    }

    // -----------------------------------------------------------------------
    // 409 Conflict — Category in use (has associated expenses)
    // -----------------------------------------------------------------------
    @ExceptionHandler(CategoryInUseException.class)
    public ResponseEntity<Map<String, Object>> handleCategoryInUse(
            CategoryInUseException ex,
            HttpServletRequest request) {

        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI());
    }

    // -----------------------------------------------------------------------
    // 400 Bad Request — Gmail account not connected
    // -----------------------------------------------------------------------
    @ExceptionHandler(GmailNotConnectedException.class)
    public ResponseEntity<Map<String, Object>> handleGmailNotConnected(
            GmailNotConnectedException ex,
            HttpServletRequest request) {

        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    // -----------------------------------------------------------------------
    // 409 Conflict — Sync already in progress
    // -----------------------------------------------------------------------
    @ExceptionHandler(SyncInProgressException.class)
    public ResponseEntity<Map<String, Object>> handleSyncInProgress(
            SyncInProgressException ex,
            HttpServletRequest request) {

        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI());
    }

    // -----------------------------------------------------------------------
    // 502 Bad Gateway — Gmail API error
    // -----------------------------------------------------------------------
    @ExceptionHandler(GmailApiException.class)
    public ResponseEntity<Map<String, Object>> handleGmailApiException(
            GmailApiException ex,
            HttpServletRequest request) {

        return buildResponse(HttpStatus.BAD_GATEWAY, ex.getMessage(), request.getRequestURI());
    }

    // -----------------------------------------------------------------------
    // 504 Gateway Timeout — Sync timeout
    // -----------------------------------------------------------------------
    @ExceptionHandler(SyncTimeoutException.class)
    public ResponseEntity<Map<String, Object>> handleSyncTimeout(
            SyncTimeoutException ex,
            HttpServletRequest request) {

        return buildResponse(HttpStatus.GATEWAY_TIMEOUT, ex.getMessage(), request.getRequestURI());
    }

    // -----------------------------------------------------------------------
    // 500 Internal Server Error — DB constraint violation
    // -----------------------------------------------------------------------
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "A database constraint was violated",
                request.getRequestURI());
    }

    // -----------------------------------------------------------------------
    // 500 Internal Server Error — Catch-all
    // -----------------------------------------------------------------------
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred",
                request.getRequestURI());
    }

    // -----------------------------------------------------------------------
    // Helper — builds the standard error envelope
    // -----------------------------------------------------------------------
    private ResponseEntity<Map<String, Object>> buildResponse(
            HttpStatus status,
            String message,
            String path) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", path);

        return ResponseEntity.status(status).body(body);
    }
}
