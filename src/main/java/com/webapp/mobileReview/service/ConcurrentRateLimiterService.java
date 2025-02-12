package com.webapp.mobileReview.service;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.Queue;

@Service
public class ConcurrentRateLimiterService implements RateLimiterService {
    private static final int MAX_REQUESTS = 5; // 5 requests per minute
    private static final long RATE_LIMIT_DURATION = 60; // seconds

    private final Queue<LocalDateTime> requestTimestamps = new LinkedList<>();

    public synchronized boolean allowRequest() {
        LocalDateTime now = LocalDateTime.now();

        // Remove timestamps older than the rate limit duration
        requestTimestamps.removeIf(timestamp ->
                timestamp.isBefore(now.minusSeconds(RATE_LIMIT_DURATION))
        );

        // Check if we've exceeded max requests
        if (requestTimestamps.size() >= MAX_REQUESTS) {
            return false;
        }

        // Add current timestamp and allow request
        requestTimestamps.offer(now);
        return true;
    }
}