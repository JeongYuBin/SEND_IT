ALTER TABLE shared_contents
    ADD COLUMN media_duration_seconds DOUBLE PRECISION,
    ADD COLUMN media_frame_keys TEXT,
    ADD COLUMN media_audio_storage_key VARCHAR(255);

