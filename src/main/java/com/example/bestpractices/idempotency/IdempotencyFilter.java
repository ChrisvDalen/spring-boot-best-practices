package com.example.bestpractices.idempotency;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Best practices demonstrated:
 * - Only intercepts POST requests — idempotency is meaningful on non-safe mutations
 * - Idempotency-Key header is optional; missing it falls through to normal processing
 * - ContentCachingResponseWrapper captures the response body without consuming it,
 *   then copies it to the real output stream at the end — transparent to controllers
 * - Three-state protocol: MISSING (process), IN_FLIGHT (409), COMPLETED (200 replay)
 * - On non-2xx response the key is released so clients can retry with the same key
 * - @Order(1) runs before Spring Security filters — adjust if auth is required first
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class IdempotencyFilter implements Filter {

    static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final IdempotencyService idempotencyService;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String idempotencyKey = request.getHeader(IDEMPOTENCY_KEY_HEADER);
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            chain.doFilter(request, response);
            return;
        }

        // Case 1: cached response — replay it
        var cached = idempotencyService.getCachedResponse(idempotencyKey);
        if (cached.isPresent()) {
            log.debug("Idempotency cache hit for key={}", idempotencyKey);
            writeJson(response, HttpStatus.OK.value(), cached.get());
            return;
        }

        // Case 2: another request with the same key is currently in-flight
        if (idempotencyService.isInFlight(idempotencyKey)) {
            log.warn("Concurrent request detected for idempotency key={}", idempotencyKey);
            writeJson(response, HttpStatus.CONFLICT.value(),
                    """
                    {"error":"A request with this Idempotency-Key is already being processed"}
                    """.strip());
            return;
        }

        // Case 3: new key — claim it and process normally
        if (!idempotencyService.claimKey(idempotencyKey)) {
            // Lost the race between isInFlight check and claimKey (very rare)
            writeJson(response, HttpStatus.CONFLICT.value(),
                    """
                    {"error":"A request with this Idempotency-Key is already being processed"}
                    """.strip());
            return;
        }

        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        try {
            chain.doFilter(request, wrappedResponse);

            int status = wrappedResponse.getStatus();
            if (status >= 200 && status < 300) {
                String body = new String(wrappedResponse.getContentAsByteArray(), StandardCharsets.UTF_8);
                idempotencyService.storeResponse(idempotencyKey, body);
                log.debug("Stored idempotency response for key={} status={}", idempotencyKey, status);
            } else {
                // Non-success: release key so caller can retry
                idempotencyService.releaseKey(idempotencyKey);
            }
        } finally {
            wrappedResponse.copyBodyToResponse();
        }
    }

    private void writeJson(HttpServletResponse response, int status, String body)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(body);
    }
}
