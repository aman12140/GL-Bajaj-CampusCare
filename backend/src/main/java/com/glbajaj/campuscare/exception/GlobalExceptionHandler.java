package com.glbajaj.campuscare.exception;

import com.glbajaj.campuscare.dto.CommonDtos.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** Centralised error handling: every failure is returned as the same JSON shape (no stack traces reach the client). */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, Map<String, String> errors) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(false, message, LocalDateTime.now(), status.value(), errors));
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException ex) {
        return build(ex.getStatus(), ex.getMessage(), null);
    }

    /** Covers @Valid failures on JSON bodies and on form (multipart) models. */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBind(BindException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        String first = errors.values().stream().findFirst().orElse("Validation failed");
        return build(HttpStatus.BAD_REQUEST, first, errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(ConstraintViolationException ex) {
        return build(HttpStatus.BAD_REQUEST, "Validation failed: " + ex.getMessage(), null);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> handleBadInput(Exception ex) {
        return build(HttpStatus.BAD_REQUEST, "Invalid or missing request data.", null);
    }

    @ExceptionHandler({MaxUploadSizeExceededException.class})
    public ResponseEntity<ErrorResponse> handleTooLarge(Exception ex) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "File too large. Maximum allowed size is 5 MB.", null);
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ErrorResponse> handleMultipart(MultipartException ex) {
        return build(HttpStatus.BAD_REQUEST, "File upload failed. Please try again with a JPG or PNG image up to 5 MB.", null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuth(AuthenticationException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Authentication required. Please log in.", null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "You do not have permission to perform this action.", null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, "This record conflicts with existing data (duplicate or still in use).", null);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDb(DataAccessException ex) {
        log.error("Database error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "A database error occurred. Please try again later.", null);
    }

    @ExceptionHandler({HttpRequestMethodNotSupportedException.class})
    public ResponseEntity<ErrorResponse> handleMethod(Exception ex) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "This HTTP method is not supported for the endpoint.", null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "Resource not found.", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOther(Exception ex) {
        log.error("Unexpected error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong on the server. Please try again.", null);
    }
}
