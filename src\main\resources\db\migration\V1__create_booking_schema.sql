CREATE TABLE app_users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(80) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(16) NOT NULL CHECK (role IN ('USER', 'ADMIN')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE events (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    starts_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE seats (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    seat_number VARCHAR(24) NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('AVAILABLE', 'HELD', 'BOOKED')),
    version BIGINT NOT NULL DEFAULT 0,
    held_by VARCHAR(80) REFERENCES app_users(username),
    hold_token UUID,
    hold_expires_at TIMESTAMPTZ,
    CONSTRAINT uq_seats_event_number UNIQUE (event_id, seat_number),
    CONSTRAINT ck_seat_hold_state CHECK (
      (status = 'HELD' AND held_by IS NOT NULL AND hold_token IS NOT NULL AND hold_expires_at IS NOT NULL)
      OR (status <> 'HELD' AND held_by IS NULL AND hold_token IS NULL AND hold_expires_at IS NULL)
    )
);
CREATE INDEX ix_seats_event_status ON seats(event_id, status);

CREATE TABLE bookings (
    id UUID PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES events(id),
    seat_id BIGINT NOT NULL UNIQUE REFERENCES seats(id),
    username VARCHAR(80) NOT NULL REFERENCES app_users(username),
    idempotency_key VARCHAR(128) NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('CONFIRMED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_booking_user_idempotency UNIQUE (username, idempotency_key)
);
CREATE INDEX ix_bookings_user_created ON bookings(username, created_at DESC);
