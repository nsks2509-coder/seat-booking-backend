<div align="center">

# Seat Booking API

**A concurrency-focused ticket reservation backend built with Spring Boot, PostgreSQL, and Redis.**

[![Java CI](https://github.com/nsks2509-coder/seat-booking-backend/actions/workflows/ci.yml/badge.svg)](https://github.com/nsks2509-coder/seat-booking-backend/actions/workflows/ci.yml)

</div>

This project models the hard part of ticketing: many customers competing for a small number of seats. A short-lived hold gives a customer time to complete checkout; PostgreSQL locking and constraints make the final booking authoritative, even when requests race.

## Design at a glance

- **Pessimistic locking for holds:** PostgreSQL `SELECT ... FOR UPDATE` serializes attempts to hold the same seat. Holds carry a random token and expire after a configurable interval.
- **Optimistic locking for confirmation:** `@Version` detects concurrent updates to a seat. A database `UNIQUE(bookings.seat_id)` constraint is the final no-double-booking guard.
- **Idempotent booking requests:** `Idempotency-Key` is unique per customer. Retrying the same key and seat returns the original booking; reusing it for a different seat conflicts.
- **Redis rate limiting:** one Lua script increments per-user request counters and assigns their expiry atomically.
- **Redis caching:** event and seat listings use a short TTL and are evicted after event, hold, and booking writes.
- **JWT and roles:** password hashes use BCrypt; access tokens are signed with HS256; only the `ADMIN` role can create events.
- **Database migrations:** Flyway owns the schema; Hibernate validates it at startup.

## Run locally

Requirements: Java 21, Maven 3.9+, Docker with Compose.

```bash
Copy-Item .env.example .env   # PowerShell
# cp .env.example .env        # macOS/Linux
```

Replace the placeholder values in `.env`. Use a unique JWT secret of at least 32 bytes and a fresh admin password of 12–72 characters. Then start the stack:

```bash
docker compose up --build
```

The API listens on `http://localhost:8080`; PostgreSQL and Redis stay on the private Compose network. Flyway creates the schema on first boot. The admin account is provisioned only when `ADMIN_USERNAME` and `ADMIN_PASSWORD` are set.

## API flow

Register a customer and create an admin token:

```bash
curl -s http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"customer1","password":"a-long-local-password"}'

curl -s http://localhost:8080/api/auth/token \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"your-admin-password"}'
```

Create an event as admin, browse public seats, then hold and confirm a seat as the customer:

```bash
curl -s http://localhost:8080/api/admin/events \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H 'Content-Type: application/json' \
  -d '{"name":"Community Tech Night","startsAt":"2030-04-01T18:00:00Z","seatCount":100}'

curl -s http://localhost:8080/api/events/1/seats

curl -s -X POST http://localhost:8080/api/events/1/seats/1/holds \
  -H "Authorization: Bearer $CUSTOMER_TOKEN"

curl -s -X POST 'http://localhost:8080/api/events/1/seats/1/bookings?holdToken=HOLD_TOKEN' \
  -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -H 'Idempotency-Key: checkout-2026-0001'
```

Endpoints:

| Method | Path | Access | Purpose |
| --- | --- | --- | --- |
| `POST` | `/api/auth/register` | Public | Create a `USER` account and receive a JWT |
| `POST` | `/api/auth/token` | Public | Exchange username/password for a JWT |
| `GET` | `/api/events` | Public | List events and current seat counts |
| `GET` | `/api/events/{eventId}/seats` | Public | List seats; expired holds appear available |
| `POST` | `/api/admin/events` | `ADMIN` | Create an event and its seats |
| `POST` | `/api/events/{eventId}/seats/{seatId}/holds` | Authenticated | Hold one seat and receive a short-lived token |
| `POST` | `/api/events/{eventId}/seats/{seatId}/bookings?holdToken=…` | Authenticated | Confirm a valid hold; requires `Idempotency-Key` |
| `GET` | `/api/bookings/me` | Authenticated | List the caller's bookings |

All errors use a small JSON response with an HTTP status, message, timestamp, and request path. User credentials, hold ownership, and booking details are never returned from the public catalog endpoints.

## Tests and concurrency proof

```bash
mvn -B verify
```

The integration tests start real PostgreSQL and Redis containers with Testcontainers. They issue parallel booking requests for one held seat and assert exactly one booking exists, then retry an idempotency key and assert the original booking is returned. Docker must be running for those tests; the tests are skipped automatically when Docker is unavailable.

Run an HTTP-level contention check against the Compose stack:

```bash
RATE_LIMIT_BOOKINGS=1000 docker compose up -d --build
ADMIN_USERNAME=admin ADMIN_PASSWORD='your-admin-password' CONTENDERS=200 \
  python scripts/concurrency-smoke.py
```

The script creates a throwaway event and customer, releases concurrent requests against one hold, and reports winners, conflicts, and rate-limit responses. It fails unless exactly one booking succeeds. It does not fabricate throughput numbers; record the host, container limits, contender count, and output when publishing a load-test result.

## Configuration

| Variable | Default | Notes |
| --- | --- | --- |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/booking` | PostgreSQL JDBC URL |
| `DATABASE_USERNAME` / `DATABASE_PASSWORD` | `booking` / `booking` | Local defaults; set secrets outside local development |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` | Redis connection |
| `JWT_SECRET` | Local-only development value | Use a random secret of at least 32 bytes in deployment |
| `JWT_TOKEN_TTL` | `PT30M` | Access token lifetime |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | empty | Provision an administrator on first startup |
| `HOLD_TTL` | `PT10M` | Seat-hold duration |
| `RATE_LIMIT_HOLDS` | `20` | Per-user holds in the rolling fixed window |
| `RATE_LIMIT_BOOKINGS` | `10` | Per-user booking attempts in the fixed window |
| `DB_POOL_SIZE` | `20` | Hikari connection pool size |

## Production notes

This is a backend portfolio project, not a ready-made ticketing platform. It has no payment provider, cancellation/refund workflow, email delivery, or multi-region coordination. The demo admin password and JWT secret must be replaced before deployment. Use TLS, managed PostgreSQL/Redis, backups, monitoring, secret rotation, and abuse controls before handling real customers or payments.

## License

MIT. See [LICENSE](LICENSE).

