package com.dagantara.backend.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/** Shared RFC 9457 error body used by MVC and Spring Security error paths. */
public final class ProblemDetails {
    private ProblemDetails() {
    }

    public static ProblemDetail of(
            HttpStatus status, String errorCode, String detail, HttpServletRequest request
    ) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setTitle(status.getReasonPhrase());
        body.setInstance(URI.create(request.getRequestURI()));
        body.setProperty("errorCode", errorCode);
        String requestId = MDC.get("requestId");
        if (requestId != null && !requestId.isBlank()) {
            body.setProperty("requestId", requestId);
        }
        return body;
    }
}
