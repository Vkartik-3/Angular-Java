package com.example.order.presentation.exception;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(
        int status,
        String code,
        String message,
        LocalDateTime timestamp,
        String correlationId,
        List<String> errors
) {

    public static ErrorResponse of(int status, String code, String message, String correlationId) {
        return new ErrorResponse(status, code, message, LocalDateTime.now(), correlationId, null);
    }

    public static ErrorResponse of(int status, String code, String message, String correlationId, List<String> errors) {
        return new ErrorResponse(status, code, message, LocalDateTime.now(), correlationId, errors);
    }
}