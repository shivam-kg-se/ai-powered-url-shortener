package com.uss.controller;

import com.uss.bindings.ApiErrorResponse;
import com.uss.service.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.HttpMediaTypeNotSupportedException;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(ApiException exception, HttpServletRequest request) {
        return error(exception.getStatus(), exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception,
                                                              HttpServletRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return error(HttpStatus.BAD_REQUEST, "Request validation failed", request, fields);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MaxUploadSizeExceededException.class})
    public ResponseEntity<ApiErrorResponse> handleMalformedRequest(Exception exception, HttpServletRequest request) {
        HttpStatus status = exception instanceof MaxUploadSizeExceededException
                ? HttpStatus.PAYLOAD_TOO_LARGE : HttpStatus.BAD_REQUEST;
        return error(status, status == HttpStatus.BAD_REQUEST ? "Malformed request body" : "Uploaded file is too large",
                request, Map.of());
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingPart(MissingServletRequestPartException exception,
                                                               HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Required multipart field is missing: " + exception.getRequestPartName(),
                request, Map.of());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnsupportedMedia(HttpMediaTypeNotSupportedException exception,
                                                                    HttpServletRequest request) {
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported Content-Type", request, Map.of());
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ApiErrorResponse> handleStorageError(IOException exception, HttpServletRequest request) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Profile picture storage operation failed",
                request, Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(DataIntegrityViolationException exception,
                                                            HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "A record with the supplied email or username already exists",
                request, Map.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthentication(AuthenticationException exception,
                                                                  HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, "Invalid username or password", request, Map.of());
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String message,
                                                    HttpServletRequest request, Map<String, String> fields) {
        ApiErrorResponse body = new ApiErrorResponse(Instant.now(), status.value(),
                status.getReasonPhrase(), message, request.getRequestURI(), fields);
        return ResponseEntity.status(status).body(body);
    }
}
