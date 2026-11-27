package dev.sriram.booking.web;

import dev.sriram.booking.service.EventCatalogService;
import dev.sriram.booking.web.dto.EventSummary;
import dev.sriram.booking.web.dto.SeatView;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
public class EventController {
    private final EventCatalogService catalog;

    public EventController(EventCatalogService catalog) { this.catalog = catalog; }

    @GetMapping
    public List<EventSummary> events() { return catalog.events(); }

    @GetMapping("/{eventId}/seats")
    public List<SeatView> seats(@PathVariable long eventId) { return catalog.seats(eventId); }
}
