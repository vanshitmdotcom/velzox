package com.velzox.apimonitor.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * API Response Wrapper - Standardized response format for all API endpoints
 * 
 * RESPONSE FORMAT:
 * {
 *   "success": true/false,
 *   "message": "Human readable message",
 *   "data": { ... },  // Only on success
 *   "errors": [ ... ] // Only on failure
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /**
     * Whether the request was successful
     */
    private boolean success;

    /**
     * Human-readable message
     */
    private String message;

    /**
     * Response data (null on error)
     */
    private T data;

    /**
     * List of errors (null on success)
     */
    private List<ErrorDetail> errors;

    /**
     * Timestamp of the response
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * Create a success response with data
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .build();
    }

    /**
     * Create a success response with message and data
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * Create a success response with just a message
     */
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .build();
    }

    /**
     * Create an error response with message
     */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .build();
    }

    /**
     * Create an error response with message and error details
     */
    public static <T> ApiResponse<T> error(String message, List<ErrorDetail> errors) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .errors(errors)
                .build();
    }

    /**
     * Error Detail - Specific error information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorDetail {
        /**
         * Field that caused the error (for validation errors)
         */
        private String field;

        /**
         * Error code for programmatic handling
         */
        private String code;

        /**
         * Human-readable error message
         */
        private String message;

        /**
         * Create error detail for field validation
         */
        public static ErrorDetail of(String field, String message) {
            return ErrorDetail.builder()
                    .field(field)
                    .message(message)
                    .build();
        }

        /**
         * Create error detail with code
         */
        public static ErrorDetail of(String field, String code, String message) {
            return ErrorDetail.builder()
                    .field(field)
                    .code(code)
                    .message(message)
                    .build();
        }
    }
}
