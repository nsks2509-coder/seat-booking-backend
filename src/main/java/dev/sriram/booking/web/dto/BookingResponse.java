package dev.sriram.booking.web.dto;

import dev.sriram.booking.domain.Booking;
import dev.sriram.booking.domain.BookingStatus;
import java.time.Instant;
import java.util.UUID;

public record BookingResponse(UUID id, long eventId, long seatId, String seatNumber,
                              BookingStatus status, Instant createdAt) {
    public static BookingResponse from(Booking booking) {
        return new BookingResponse(booking.getId(), booking.getEvent().getId(), booking.getSeat().getId(),
                booking.getSeat().getSeatNumber(), booking.getStatus(), booking.getCreatedAt());
    }
}
