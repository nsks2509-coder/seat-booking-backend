package dev.sriram.booking.web;

import dev.sriram.booking.service.BookingService;
import dev.sriram.booking.web.dto.BookingResponse;
import dev.sriram.booking.web.dto.HoldResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class BookingController {
    private final BookingService bookings;

    public BookingController(BookingService bookings) { this.bookings = bookings; }

    @PostMapping("/events/{eventId}/seats/{seatId}/holds")
    public HoldResponse hold(@PathVariable long eventId, @PathVariable long seatId, Authentication authentication) {
        return bookings.hold(authentication.getName(), eventId, seatId);
    }

    @PostMapping("/events/{eventId}/seats/{seatId}/bookings")
    public ResponseEntity<BookingResponse> book(@PathVariable long eventId, @PathVariable long seatId,
            @RequestParam UUID holdToken, @RequestHeader("Idempotency-Key") String idempotencyKey,
            Authentication authentication) {
        BookingResponse result = bookings.book(authentication.getName(), eventId, seatId, holdToken, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/bookings/me")
    public List<BookingResponse> myBookings(Authentication authentication) {
        return bookings.myBookings(authentication.getName());
    }
}
