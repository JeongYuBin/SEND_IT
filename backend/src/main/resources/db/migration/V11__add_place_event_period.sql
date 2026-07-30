ALTER TABLE places
    ADD COLUMN event_start_date DATE,
    ADD COLUMN event_end_date DATE;

CREATE INDEX idx_places_event_period
    ON places (event_start_date, event_end_date)
    WHERE event_start_date IS NOT NULL OR event_end_date IS NOT NULL;
