package dev.sriram.booking.service;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class RateLimitService {
    private final StringRedisTemplate redis;
    private final DefaultRedisScript<Long> script;
    private final Duration window;
    private final long holdsPerWindow;
    private final long bookingsPerWindow;

    public RateLimitService(StringRedisTemplate redis, DefaultRedisScript<Long> rateLimitScript,
                            @Value("${app.booking.rate-limit.window}") Duration window,
                            @Value("${app.booking.rate-limit.holds-per-window}") long holdsPerWindow,
                            @Value("${app.booking.rate-limit.bookings-per-window}") long bookingsPerWindow) {
        this.redis = redis;
        this.script = rateLimitScript;
        this.window = window;
        this.holdsPerWindow = holdsPerWindow;
        this.bookingsPerWindow = bookingsPerWindow;
    }

    public void check(String username, String operation) {
        long limit = "hold".equals(operation) ? holdsPerWindow : bookingsPerWindow;
        Long count = redis.execute(script, java.util.List.of("booking:rate:" + operation + ":" + username),
                Long.toString(window.toMillis()));
        if (count == null) throw new IllegalStateException("Redis rate limiter did not return a count");
        if (count > limit) throw new RateLimitExceededException();
    }
}
