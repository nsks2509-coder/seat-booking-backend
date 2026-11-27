package dev.sriram.booking.repository;

import dev.sriram.booking.domain.Seat;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    @Query("select s from Seat s where s.event.id = :eventId order by s.seatNumber")
    List<Seat> findAllForEvent(@Param("eventId") long eventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Seat s where s.id = :seatId and s.event.id = :eventId")
    Optional<Seat> lockSeat(@Param("eventId") long eventId, @Param("seatId") long seatId);

    @Query("select count(s) from Seat s where s.event.id = :eventId")
    long countForEvent(@Param("eventId") long eventId);
}
