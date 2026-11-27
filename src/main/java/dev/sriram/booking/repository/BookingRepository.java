package dev.sriram.booking.repository;

import dev.sriram.booking.domain.Booking;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, UUID> {
    Optional<Booking> findByUsernameAndIdempotencyKey(String username, String idempotencyKey);
    List<Booking> findAllByUsernameOrderByCreatedAtDesc(String username);
    @Query("select count(b) from Booking b where b.seat.id = :seatId")
    long countBySeatId(@Param("seatId") long seatId);
}
