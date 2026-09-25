package com.shreeassociates.exception;

import com.shreeassociates.dto.EnquiryResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Every exception is caught here so that the customer-facing API never leaks
 * stack traces, SQL details, or credentials - only a friendly message and,
 * for validation errors, a map of field -> problem.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Bean Validation failures on @Valid @RequestBody DTOs. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fe ->
                fieldErrors.put(fe.getField(), fe.getDefaultMessage()));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("message", "Please correct the highlighted fields and try again.");
        body.put("errors", fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<EnquiryResponse> handleConstraintViolation(ConstraintViolationException ex) {
        return ResponseEntity.badRequest()
                .body(EnquiryResponse.error("Some of the submitted information is invalid. Please check and try again."));
    }

    /** Malformed / non-JSON request bodies. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<EnquiryResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        log.warn("Malformed enquiry request body: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(EnquiryResponse.error("We couldn't read your submission. Please try again."));
    }

    @ExceptionHandler(DuplicateEnquiryException.class)
    public ResponseEntity<EnquiryResponse> handleDuplicate(DuplicateEnquiryException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(EnquiryResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<EnquiryResponse> handleRateLimit(RateLimitExceededException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(EnquiryResponse.error(ex.getMessage()));
    }

    /** Database connectivity / query failures - never surface SQL details to the client. */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<EnquiryResponse> handleDatabase(DataAccessException ex) {
        log.error("Database error while processing enquiry", ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(EnquiryResponse.error("We're temporarily unable to process your enquiry. Please try again shortly or contact us by phone."));
    }

    /** Catch-all safety net - logs full details server-side, returns a generic message to the client. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<EnquiryResponse> handleGeneric(Exception ex) {
        log.error("Unexpected error while processing enquiry", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(EnquiryResponse.error("Something went wrong on our end. Please try again later or contact us directly."));
    }
}
