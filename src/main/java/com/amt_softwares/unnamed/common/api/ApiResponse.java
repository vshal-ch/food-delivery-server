package com.amt_softwares.unnamed.common.api;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private List<String> errors;

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .errors(List.of())
                .build();
    }

    public static ApiResponse<Void> error(String message, List<String> errors) {
        return ApiResponse.<Void>builder()
                .success(false)
                .message(message)
                .data(null)
                .errors(errors)
                .build();
    }
}
