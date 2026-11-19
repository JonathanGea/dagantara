package com.dagantara.backend.common.api;

import java.util.List;
import org.springframework.data.domain.Page;

/** Converts a Spring Data page to stable, transport-oriented pagination metadata. */
public record PageResponse<T>(List<T> items, Pagination meta) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                new Pagination(
                        page.getNumber(),
                        page.getSize(),
                        page.getTotalElements(),
                        page.getTotalPages(),
                        page.isFirst(),
                        page.isLast()
                )
        );
    }

    public record Pagination(
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean first,
            boolean last
    ) {
    }
}
