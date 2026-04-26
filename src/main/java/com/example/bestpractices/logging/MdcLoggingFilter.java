package com.example.bestpractices.logging;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Best practices demonstrated:
 * - Servlet filter runs before Spring's DispatcherServlet — every request gets a correlation ID
 * - Check for an upstream X-Correlation-ID first so distributed traces are preserved end-to-end
 * - Store the ID in MDC (Mapped Diagnostic Context) so every log line in the request thread
 *   includes it automatically (add %X{correlationId} to your logback pattern)
 * - Echo the correlation ID back in the response header so clients can reference it in bug reports
 * - Always clean MDC in finally to prevent ID leaking to the next request on a pooled thread
 * - @Order(Ordered.HIGHEST_PRECEDENCE) ensures this runs before other filters
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcLoggingFilter implements Filter {

    private static final String CORRELATION_HEADER = "X-Correlation-ID";
    private static final String MDC_KEY = "correlationId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String correlationId = httpRequest.getHeader(CORRELATION_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_KEY, correlationId);
        httpResponse.setHeader(CORRELATION_HEADER, correlationId);

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
