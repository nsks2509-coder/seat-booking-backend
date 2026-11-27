package dev.sriram.booking.repository;

import dev.sriram.booking.domain.Event;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findAllByOrderByStartsAtAsc();
}
