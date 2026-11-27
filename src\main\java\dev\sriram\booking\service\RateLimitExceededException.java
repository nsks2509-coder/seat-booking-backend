package dev.sriram.booking.service;

import org.springframework.http.HttpStatus;

public final class RateLimitExceededException extends ApiException {
    public RateLimitExceededException() { super(HttpStatus.TOO_MANY_REQUESTS, "Request rate limit exceeded"); }
}
