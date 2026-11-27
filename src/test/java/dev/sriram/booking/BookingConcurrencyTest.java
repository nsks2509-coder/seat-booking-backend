package dev.sriram.booking;

import static org.assertj.core.api.Assertions.assertThat;

import dev.sriram.booking.domain.AppUser;
import dev.sriram.booking.domain.Event;
import dev.sriram.booking.domain.Seat;
import dev.sriram.booking.domain.SeatStatus;
import dev.sriram.booking.repository.AppUserRepository;
import dev.sriram.booking.repository.BookingRepository;
import dev.sriram.booking.repository.EventRepository;
import dev.sriram.booking.repository.SeatRepository;
import dev.sriram.booking.service.BookingService;
import dev.sriram.booking.web.dto.BookingResponse;
import dev.sriram.booking.web.dto.HoldResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.containers.wait.strategy.Wait;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class BookingConcurrencyTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"))
            .withDatabaseName("booking")
            .withUsername("booking")
            .withPassword("booking-test-password");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
            .withExposedPorts(6379)
            .waitingFor(Wait.forListeningPort());

    @DynamicPropertySource
    static void dependencies(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("app.security.jwt-secret", () -> "integration-test-secret-with-more-than-32-bytes");
        registry.add("app.booking.rate-limit.bookings-per-window", () -> "100");
    }

    @Autowired BookingService bookingService;
    @Autowired AppUserRepository users;
    @Autowired EventRepository events;
    @Autowired SeatRepository seats;
    @Autowired BookingRepository bookings;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void parallelBookingRequestsNeverDoubleBookOneSeat() throws Exception {
        Fixture fixture = fixture();
        HoldResponse hold = bookingService.hold(fixture.username(), fixture.eventId(), fixture.seatId());

        int contenders = 8;
        ExecutorService executor = Executors.newFixedThreadPool(contenders);
        CountDownLatch ready = new CountDownLatch(contenders);
        CountDownLatch start = new CountDownLatch(1);
        List<CompletableFuture<Boolean>> attempts = new ArrayList<>();
        for (int index = 0; index < contenders; index++) {
            int request = index;
            attempts.add(CompletableFuture.supplyAsync(() -> {
                ready.countDown();
                try {
                    if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("barrier timeout");
                    BookingResponse result = bookingService.book(fixture.username(), fixture.eventId(), fixture.seatId(),
                            hold.holdToken(), "parallel-request-" + request + "-" + UUID.randomUUID());
                    return result.status().name().equals("CONFIRMED");
                } catch (RuntimeException expectedConflict) {
                    return false;
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(interrupted);
                }
            }, executor));
        }

        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        long successful = attempts.stream().filter(CompletableFuture::join).count();
        executor.shutdownNow();

        assertThat(successful).isEqualTo(1);
        assertThat(bookings.countBySeatId(fixture.seatId())).isEqualTo(1);
        assertThat(seats.findById(fixture.seatId()).orElseThrow().getStatus()).isEqualTo(SeatStatus.BOOKED);
    }

    @Test
    void replayingSameIdempotencyKeyReturnsTheOriginalBooking() {
        Fixture fixture = fixture();
        HoldResponse hold = bookingService.hold(fixture.username(), fixture.eventId(), fixture.seatId());
        String key = "retry-key-" + UUID.randomUUID();

        BookingResponse first = bookingService.book(fixture.username(), fixture.eventId(), fixture.seatId(), hold.holdToken(), key);
        BookingResponse retry = bookingService.book(fixture.username(), fixture.eventId(), fixture.seatId(), hold.holdToken(), key);

        assertThat(retry.id()).isEqualTo(first.id());
        assertThat(bookings.countBySeatId(fixture.seatId())).isEqualTo(1);
    }

    private Fixture fixture() {
        String username = "customer-" + UUID.randomUUID();
        users.saveAndFlush(new AppUser(username, passwordEncoder.encode("a-long-test-password"), "USER"));
        Event event = events.saveAndFlush(new Event("Concurrency test " + UUID.randomUUID(), Instant.now().plusSeconds(86_400)));
        Seat seat = seats.saveAndFlush(new Seat(event, "A-0001"));
        return new Fixture(username, event.getId(), seat.getId());
    }

    private record Fixture(String username, long eventId, long seatId) {}
}
