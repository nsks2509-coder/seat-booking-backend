package dev.sriram.booking.web;

import dev.sriram.booking.service.AdminEventService;
import dev.sriram.booking.web.dto.EventCreateRequest;
import dev.sriram.booking.web.dto.EventSummary;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/events")
@PreAuthorize("hasRole('ADMIN')")
public class AdminEventController {
    private final AdminEventService events;

    public AdminEventController(AdminEventService events) { this.events = events; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventSummary create(@Valid @RequestBody EventCreateRequest request) { return events.create(request); }
}
