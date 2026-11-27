package dev.sriram.booking.service;

import dev.sriram.booking.domain.SeatStatus;
import dev.sriram.booking.repository.EventRepository;
import dev.sriram.booking.repository.SeatRepository;
import dev.sriram.booking.web.dto.EventSummary;
import dev.sriram.booking.web.dto.SeatView;
import java.time.Instant;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventCatalogService {
    private final EventRepository events;
    private final SeatRepository seats;

    public EventCatalogService(EventRepository events, SeatRepository seats) {
        this.events = events;
        this.seats = seats;
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "events", key = "'all'")
    public List<EventSummary> events() {
        return events.findAllByOrderByStartsAtAsc().stream().map(event -> {
            var eventSeats = seats.findAllForEvent(event.getId());
            long available = eventSeats.stream().filter(seat -> seat.visibleStatus(Instant.now()) == SeatStatus.AVAILABLE).count();
            return new EventSummary(event.getId(), event.getName(), event.getStartsAt(), eventSeats.size(), available);
        }).toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "event-seats", key = "#eventId")
    public List<SeatView> seats(long eventId) {
        if (!events.existsById(eventId)) throw new NotFoundException("Event not found");
        Instant now = Instant.now();
        return seats.findAllForEvent(eventId).stream()
                .map(seat -> new SeatView(seat.getId(), seat.getSeatNumber(), seat.visibleStatus(now), seat.getVersion()))
                .toList();
    }
}
