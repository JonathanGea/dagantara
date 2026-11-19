package com.dagantara.backend.common.api;

import com.dagantara.backend.common.exception.ApiException;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

/** Apply this explicitly in list services, with an allowlist appropriate to each entity. */
public final class PageableRules {
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private PageableRules() {
    }

    public static Pageable enforce(Pageable requested, Set<String> allowedSortFields) {
        if (requested == null || requested.isUnpaged()) {
            return PageRequest.of(0, DEFAULT_SIZE);
        }

        if (requested.getPageNumber() < 0 || requested.getPageSize() < 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PAGINATION",
                    "Page must be non-negative and size must be positive");
        }

        if (allowedSortFields == null) {
            throw new IllegalArgumentException("An explicit sort-field allowlist is required");
        }

        requested.getSort().forEach(order -> {
            if (!allowedSortFields.contains(order.getProperty())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_SORT",
                        "Unsupported sort property: " + order.getProperty());
            }
        });

        return PageRequest.of(
                requested.getPageNumber(),
                Math.min(requested.getPageSize(), MAX_SIZE),
                requested.getSort()
        );
    }
}
