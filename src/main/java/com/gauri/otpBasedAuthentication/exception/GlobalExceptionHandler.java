package com.gauri.otpBasedAuthentication.exception;

import com.gauri.otpBasedAuthentication.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntime(RuntimeException e) {
        return ResponseEntity.badRequest().body(
                ErrorResponse.builder()
                        .success(false)
                        .statusCode(400)
                        .error("BAD_REQUEST")
                        .message(e.getMessage())
                        .build()
        );
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> handleResponseStatus(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(
                ErrorResponse.builder()
                        .success(false)
                        .statusCode(e.getStatusCode().value())
                        .error("UNAUTHORIZED")
                        .message(e.getReason())
                        .build()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors()
                .stream().map(f -> f.getDefaultMessage())
                .findFirst().orElse("Validation failed");
        return ResponseEntity.badRequest().body(
                ErrorResponse.builder()
                        .success(false)
                        .statusCode(400)
                        .error("VALIDATION_ERROR")
                        .message(message)
                        .build()
        );
    }
}