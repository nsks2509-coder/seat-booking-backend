package dev.sriram.booking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "seats", uniqueConstraints = @UniqueConstraint(name = "uq_seats_event_number", columnNames = {"event_id", "seat_number"}))
public class Seat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "seat_number", nullable = false, length = 24)
    private String seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SeatStatus status = SeatStatus.AVAILABLE;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "held_by", length = 80)
    private String heldBy;

    @Column(name = "hold_token")
    private UUID holdToken;

    @Column(name = "hold_expires_at")
    private Instant holdExpiresAt;

    protected Seat() {}

    public Seat(Event event, String seatNumber) {
        this.event = event;
        this.seatNumber = seatNumber;
        this.status = SeatStatus.AVAILABLE;
    }

    public void placeHold(String username, UUID token, Instant expiresAt) {
        status = SeatStatus.HELD;
        heldBy = username;
        holdToken = token;
        holdExpiresAt = expiresAt;
    }

    public void confirmBooking() {
        status = SeatStatus.BOOKED;
        heldBy = null;
        holdToken = null;
        holdExpiresAt = null;
    }

    public void releaseExpiredHold() {
        status = SeatStatus.AVAILABLE;
        heldBy = null;
        holdToken = null;
        holdExpiresAt = null;
    }

    public SeatStatus visibleStatus(Instant now) {
        return status == SeatStatus.HELD && holdExpiresAt != null && !holdExpiresAt.isAfter(now)
                ? SeatStatus.AVAILABLE : status;
    }

    public Long getId() { return id; }
    public Event getEvent() { return event; }
    public String getSeatNumber() { return seatNumber; }
    public SeatStatus getStatus() { return status; }
    public long getVersion() { return version; }
    public String getHeldBy() { return heldBy; }
    public UUID getHoldToken() { return holdToken; }
    public Instant getHoldExpiresAt() { return holdExpiresAt; }
}
