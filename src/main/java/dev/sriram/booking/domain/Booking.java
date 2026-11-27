package dev.sriram.booking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bookings", uniqueConstraints = {
        @UniqueConstraint(name = "uq_booking_user_idempotency", columnNames = {"username", "idempotency_key"})
})
public class Booking {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_id", nullable = false, unique = true)
    private Seat seat;

    @Column(nullable = false, length = 80)
    private String username;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private BookingStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Booking() {}

    public Booking(Event event, Seat seat, String username, String idempotencyKey) {
        this.id = UUID.randomUUID();
        this.event = event;
        this.seat = seat;
        this.username = username;
        this.idempotencyKey = idempotencyKey;
        this.status = BookingStatus.CONFIRMED;
    }

    public UUID getId() { return id; }
    public Event getEvent() { return event; }
    public Seat getSeat() { return seat; }
    public String getUsername() { return username; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public BookingStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
