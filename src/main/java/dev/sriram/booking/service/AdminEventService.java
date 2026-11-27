package dev.sriram.booking.service;

import dev.sriram.booking.domain.Event;
import dev.sriram.booking.domain.Seat;
import dev.sriram.booking.repository.EventRepository;
import dev.sriram.booking.repository.SeatRepository;
import dev.sriram.booking.web.dto.EventCreateRequest;
import dev.sriram.booking.web.dto.EventSummary;
import java.util.ArrayList;
import java.util.Locale;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminEventService {
    private final EventRepository events;
    private final SeatRepository seats;

    public AdminEventService(EventRepository events, SeatRepository seats) {
        this.events = events;
        this.seats = seats;
    }

    @Transactional
    @CacheEvict(cacheNames = {"events", "event-seats"}, allEntries = true)
    public EventSummary create(EventCreateRequest request) {
        Event event = events.saveAndFlush(new Event(request.name().trim(), request.startsAt()));
        var generatedSeats = new ArrayList<Seat>(request.seatCount());
        for (int number = 1; number <= request.seatCount(); number++) {
            generatedSeats.add(new Seat(event, String.format(Locale.ROOT, "A-%04d", number)));
        }
        seats.saveAll(generatedSeats);
        return new EventSummary(event.getId(), event.getName(), event.getStartsAt(), request.seatCount(), request.seatCount());
    }
}
