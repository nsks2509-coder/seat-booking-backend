package dev.sriram.booking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "events")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Event() {}

    public Event(String name, Instant startsAt) {
        this.name = name;
        this.startsAt = startsAt;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getCreatedAt() { return createdAt; }
}
