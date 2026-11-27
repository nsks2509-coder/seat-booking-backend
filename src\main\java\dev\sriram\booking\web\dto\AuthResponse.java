package dev.sriram.booking.web.dto;

import java.time.Instant;

public record AuthResponse(String accessToken, String tokenType, Instant expiresAt, String role) {}
