CREATE TABLE user_saved_place_sources (
    id BIGSERIAL PRIMARY KEY,
    saved_place_id BIGINT NOT NULL
        REFERENCES user_saved_places(id) ON DELETE CASCADE,
    shared_content_id BIGINT NOT NULL
        REFERENCES shared_contents(id) ON DELETE CASCADE,
    linked_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_saved_place_sources_content
        UNIQUE(saved_place_id, shared_content_id)
);

CREATE INDEX idx_saved_place_sources_saved
    ON user_saved_place_sources(saved_place_id, linked_at DESC);

INSERT INTO user_saved_place_sources(saved_place_id, shared_content_id, linked_at)
SELECT id, shared_content_id, saved_at
FROM user_saved_places
WHERE shared_content_id IS NOT NULL
ON CONFLICT DO NOTHING;
