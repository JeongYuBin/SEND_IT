CREATE TABLE itineraries (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(150) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    daily_start_time TIME NOT NULL,
    daily_end_time TIME NOT NULL,
    transport_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_itinerary_dates CHECK (end_date >= start_date),
    CONSTRAINT chk_itinerary_times CHECK (daily_end_time > daily_start_time)
);

CREATE INDEX idx_itineraries_user_start_date ON itineraries(user_id, start_date DESC);

CREATE TABLE itinerary_items (
    id BIGSERIAL PRIMARY KEY,
    itinerary_id BIGINT NOT NULL REFERENCES itineraries(id) ON DELETE CASCADE,
    saved_place_id BIGINT NOT NULL REFERENCES user_saved_places(id) ON DELETE CASCADE,
    sequence INTEGER NOT NULL,
    stay_minutes INTEGER NOT NULL DEFAULT 60,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_itinerary_saved_place UNIQUE(itinerary_id, saved_place_id),
    CONSTRAINT uk_itinerary_sequence UNIQUE(itinerary_id, sequence),
    CONSTRAINT chk_itinerary_sequence CHECK(sequence > 0),
    CONSTRAINT chk_itinerary_stay_minutes CHECK(stay_minutes BETWEEN 15 AND 720)
);
