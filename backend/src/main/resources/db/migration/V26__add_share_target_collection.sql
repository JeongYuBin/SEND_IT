ALTER TABLE shared_contents
    ADD COLUMN target_collection_id BIGINT REFERENCES collections(id) ON DELETE SET NULL;

CREATE INDEX idx_shared_contents_target_collection
    ON shared_contents(target_collection_id);
