package dev.sriram.booking.web.dto;

import dev.sriram.booking.domain.SeatStatus;

public record SeatView(long id, String seatNumber, SeatStatus status, long version) {}
