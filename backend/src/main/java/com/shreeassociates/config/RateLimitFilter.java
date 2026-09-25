package com.shreeassociates.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Simple fixed-window-per-IP rate limiter for POST /api/enquiries, to blunt
 * basic spam/bot abuse without adding an external dependency.
 * For high-traffic production use, replace with a proper library (e.g. Bucket4j)
 * or an API gateway / reverse-proxy level limiter (e.g. Nginx, Cloudflare).
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    @Value("${app.ratelimit.max-requests:5}")
    private int maxRequests;

    @Value("${app.ratelimit.window-seconds:60}")
    private int windowSeconds;

    private final ConcurrentHashMap<String, Deque<Instant>> requestLog = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if ("POST".equalsIgnoreCase(request.getMethod()) && "/api/enquiries".equals(request.getRequestURI())) {
            String clientIp = resolveClientIp(request);
            Instant now = Instant.now();
            Deque<Instant> timestamps = requestLog.computeIfAbsent(clientIp, k -> new ConcurrentLinkedDeque<>());

            synchronized (timestamps) {
                while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(now.minusSeconds(windowSeconds))) {
                    timestamps.pollFirst();
                }
                if (timestamps.size() >= maxRequests) {
                    response.setStatus(429); // 429 Too Many Requests - not defined as a constant in HttpServletResponse
                    response.setContentType("application/json");
                    response.getWriter().write(
                            "{\"success\":false,\"message\":\"Too many requests. Please wait a moment before submitting again.\"}");
                    return;
                }
                timestamps.addLast(now);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}