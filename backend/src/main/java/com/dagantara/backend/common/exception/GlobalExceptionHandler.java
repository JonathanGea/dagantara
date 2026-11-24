package com.dagantara.backend.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ProblemDetail handleApi(ApiException exception, HttpServletRequest request) {
        return ProblemDetails.of(exception.getStatus(), exception.getErrorCode(),
                exception.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleBodyValidation(
            MethodArgumentNotValidException exception, HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetails.of(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Request validation failed", request);
        List<FieldViolation> violations = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()))
                .toList();
        problem.setProperty("violations", violations);
        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraint(
            ConstraintViolationException exception, HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetails.of(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Request validation failed", request);
        problem.setProperty("violations", exception.getConstraintViolations().stream()
                .map(v -> new FieldViolation(v.getPropertyPath().toString(), v.getMessage()))
                .toList());
        return problem;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleParameterType(HttpServletRequest request) {
        return ProblemDetails.of(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER",
                "An endpoint parameter has the wrong type", request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleInvalidJson(HttpServletRequest request) {
        return ProblemDetails.of(HttpStatus.BAD_REQUEST, "INVALID_REQUEST_BODY",
                "Malformed or missing JSON request body", request);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ProblemDetail handleMissingRequestPart(HttpServletRequest request) {
        return ProblemDetails.of(HttpStatus.BAD_REQUEST, "INVALID_REQUEST_BODY",
                "A required form part is missing", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleIntegrity(DataIntegrityViolationException exception,
                                         HttpServletRequest request) {
        LOG.warn("A persistence constraint was violated");
        return ProblemDetails.of(HttpStatus.CONFLICT, "DATA_CONFLICT",
                "A data constraint prevents this operation", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleMissingResource(HttpServletRequest request) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, "NOT_FOUND",
                "Resource not found", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail handleMethod(HttpServletRequest request) {
        return ProblemDetails.of(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED",
                "HTTP method is not supported", request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ProblemDetail handleMedia(HttpServletRequest request) {
        return ProblemDetails.of(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE",
                "Content type is not supported", request);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception exception, HttpServletRequest request) {
        LOG.error("Unexpected request failure", exception);
        return ProblemDetails.of(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected server error occurred", request);
    }

    private record FieldViolation(String field, String message) {
    }
}
