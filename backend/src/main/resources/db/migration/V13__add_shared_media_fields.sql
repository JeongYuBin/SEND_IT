ALTER TABLE shared_contents
    ADD COLUMN media_storage_key VARCHAR(255),
    ADD COLUMN media_original_filename VARCHAR(500),
    ADD COLUMN media_content_type VARCHAR(100),
    ADD COLUMN media_size_bytes BIGINT;

