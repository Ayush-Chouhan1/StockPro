package com.stockpro.warehouseservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(WarehouseNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            WarehouseNotFoundException ex, HttpServletRequest request) {
        log.error("WarehouseNotFoundException: {}", ex.getMessage());
        return new ResponseEntity<>(
                new ErrorResponse(LocalDateTime.now(),
                        HttpStatus.NOT_FOUND.value(), "Not Found",
                        ex.getMessage(), request.getRequestURI()),
                HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientStock(
            InsufficientStockException ex, HttpServletRequest request) {
        log.error("InsufficientStockException: {}", ex.getMessage());
        return new ResponseEntity<>(
                new ErrorResponse(LocalDateTime.now(),
                        HttpStatus.BAD_REQUEST.value(), "Bad Request",
                        ex.getMessage(), request.getRequestURI()),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(
            MethodArgumentNotValidException ex) {
        log.error("Validation error: {}", ex.getMessage());
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            errors.put(field, error.getDefaultMessage());
        });
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        log.error("IllegalArgumentException: {}", ex.getMessage());
        return new ResponseEntity<>(
                new ErrorResponse(LocalDateTime.now(),
                        HttpStatus.BAD_REQUEST.value(), "Bad Request",
                        ex.getMessage(), request.getRequestURI()),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return new ResponseEntity<>(
                new ErrorResponse(LocalDateTime.now(),
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "Internal Server Error",
                        "An unexpected error occurred",
                        request.getRequestURI()),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}