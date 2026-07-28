ALTER TABLE itinerary_items
    ADD COLUMN preferred_visit_date DATE,
    ADD COLUMN preferred_start_time TIME;

ALTER TABLE itinerary_items
    ADD CONSTRAINT chk_itinerary_preferred_time
        CHECK (preferred_start_time IS NULL OR preferred_visit_date IS NOT NULL);
