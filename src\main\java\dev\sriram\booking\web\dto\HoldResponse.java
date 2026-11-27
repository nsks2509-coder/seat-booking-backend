package dev.sriram.booking.web.dto;

import java.time.Instant;
import java.util.UUID;

public record HoldResponse(long eventId, long seatId, UUID holdToken, Instant expiresAt) {}
