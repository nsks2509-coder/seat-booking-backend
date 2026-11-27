package dev.sriram.booking.web.dto;

import java.time.Instant;

public record EventSummary(long id, String name, Instant startsAt, long seatCount, long availableSeats) {}
