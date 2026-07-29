ALTER TABLE places
    ADD COLUMN tourism_content_id VARCHAR(50),
    ADD COLUMN tourism_content_type_id VARCHAR(20),
    ADD COLUMN operating_hours TEXT,
    ADD COLUMN rest_days TEXT,
    ADD COLUMN parking_info TEXT;

CREATE INDEX idx_places_tourism_content
    ON places (tourism_content_id, tourism_content_type_id);
