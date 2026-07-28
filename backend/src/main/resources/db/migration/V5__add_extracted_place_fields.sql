ALTER TABLE shared_contents
    ADD COLUMN extracted_place_name VARCHAR(200),
    ADD COLUMN extracted_category VARCHAR(100),
    ADD COLUMN extracted_address VARCHAR(500),
    ADD COLUMN extracted_latitude DOUBLE PRECISION,
    ADD COLUMN extracted_longitude DOUBLE PRECISION;

