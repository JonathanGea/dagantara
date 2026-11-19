package com.dagantara.backend.common.api;

import java.util.List;
import org.springframework.data.domain.Page;

/** Uniform envelope for successful API responses. Errors use RFC 9457 ProblemDetail. */
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        PageResponse.Pagination meta
) {
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, null);
    }

    public static <T> ApiResponse<List<T>> paged(String message, Page<T> page) {
        PageResponse<T> response = PageResponse.from(page);
        return new ApiResponse<>(true, message, response.items(), response.meta());
    }
}
