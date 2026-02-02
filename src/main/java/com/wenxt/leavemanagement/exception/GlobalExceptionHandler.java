package com.wenxt.leavemanagement.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ✅ Handle Specific Logic Errors (Like Insufficient Balance)
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(BadRequestException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Bad Request");
        response.put("message", ex.getMessage()); // This will show your custom error messages

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // ✅ Handle Validation & Runtime Errors (The fix for your 500 error)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.put("error", "Internal Server Error");

        // 🔥 CRITICAL CHANGE: Show the actual cause of the 500 error
        response.put("message", ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred");

        // Optional: Helpful for development to see which class crashed
        response.put("exceptionType", ex.getClass().getSimpleName());

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}