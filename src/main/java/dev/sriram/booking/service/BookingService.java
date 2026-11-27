package dev.sriram.booking.service;

import dev.sriram.booking.domain.Booking;
import dev.sriram.booking.domain.Seat;
import dev.sriram.booking.domain.SeatStatus;
import dev.sriram.booking.repository.BookingRepository;
import dev.sriram.booking.repository.SeatRepository;
import dev.sriram.booking.web.dto.BookingResponse;
import dev.sriram.booking.web.dto.HoldResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {
    private final SeatRepository seats;
    private final BookingRepository bookings;
    private final RateLimitService rateLimits;
    private final Duration holdTtl;

    public BookingService(SeatRepository seats, BookingRepository bookings, RateLimitService rateLimits,
                          @Value("${app.booking.hold-ttl}") Duration holdTtl) {
        this.seats = seats;
        this.bookings = bookings;
        this.rateLimits = rateLimits;
        this.holdTtl = holdTtl;
    }

    @Transactional
    @CacheEvict(cacheNames = {"events", "event-seats"}, allEntries = true)
    public HoldResponse hold(String username, long eventId, long seatId) {
        rateLimits.check(username, "hold");
        Seat seat = seats.lockSeat(eventId, seatId)
                .orElseThrow(() -> new NotFoundException("Seat not found for this event"));
        Instant now = Instant.now();
        if (seat.getStatus() == SeatStatus.BOOKED) throw new ConflictException("Seat is already booked");
        if (seat.getStatus() == SeatStatus.HELD && seat.getHoldExpiresAt() != null && seat.getHoldExpiresAt().isAfter(now)) {
            if (username.equals(seat.getHeldBy())) {
                return new HoldResponse(eventId, seatId, seat.getHoldToken(), seat.getHoldExpiresAt());
            }
            throw new ConflictException("Seat is currently held by another customer");
        }
        if (seat.getStatus() == SeatStatus.HELD) seat.releaseExpiredHold();
        UUID token = UUID.randomUUID();
        Instant expiresAt = now.plus(holdTtl);
        seat.placeHold(username, token, expiresAt);
        return new HoldResponse(eventId, seatId, token, expiresAt);
    }

    @Transactional
    @CacheEvict(cacheNames = {"events", "event-seats"}, allEntries = true)
    public BookingResponse book(String username, long eventId, long seatId, UUID holdToken, String idempotencyKey) {
        rateLimits.check(username, "booking");
        validateIdempotencyKey(idempotencyKey);
        var previous = bookings.findByUsernameAndIdempotencyKey(username, idempotencyKey);
        if (previous.isPresent()) {
            Booking booking = previous.get();
            if (booking.getEvent().getId() != eventId || booking.getSeat().getId() != seatId) {
                throw new ConflictException("Idempotency-Key was already used for a different booking");
            }
            return BookingResponse.from(booking);
        }

        Seat seat = seats.findById(seatId).orElseThrow(() -> new NotFoundException("Seat not found"));
        if (seat.getEvent().getId() != eventId) throw new NotFoundException("Seat not found for this event");
        if (seat.getStatus() == SeatStatus.BOOKED) throw new ConflictException("Seat is already booked");
        Instant now = Instant.now();
        if (seat.getStatus() != SeatStatus.HELD || !username.equals(seat.getHeldBy()) ||
                !holdToken.equals(seat.getHoldToken()) || seat.getHoldExpiresAt() == null || !seat.getHoldExpiresAt().isAfter(now)) {
            throw new ConflictException("A valid, unexpired seat hold is required");
        }

        // @Version makes competing confirmation transactions race on one row;
        // PostgreSQL also enforces UNIQUE(bookings.seat_id) as the final guard.
        seat.confirmBooking();
        seats.flush();
        Booking booking = bookings.saveAndFlush(new Booking(seat.getEvent(), seat, username, idempotencyKey));
        return BookingResponse.from(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> myBookings(String username) {
        return bookings.findAllByUsernameOrderByCreatedAtDesc(username).stream().map(BookingResponse::from).toList();
    }

    private void validateIdempotencyKey(String key) {
        if (key == null || key.length() < 8 || key.length() > 128 || key.chars().anyMatch(Character::isISOControl)) {
            throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Idempotency-Key must contain 8-128 printable characters");
        }
    }
}
