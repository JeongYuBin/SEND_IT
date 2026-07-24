CREATE TABLE shared_contents (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    original_url VARCHAR(2048) NOT NULL,
    normalized_url VARCHAR(2048) NOT NULL,
    source_type VARCHAR(30) NOT NULL,
    shared_text TEXT,
    title VARCHAR(500),
    description TEXT,
    thumbnail_url VARCHAR(2048),
    analysis_status VARCHAR(30) NOT NULL,
    analysis_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_shared_contents_user_url UNIQUE (user_id, normalized_url)
);

CREATE TABLE analysis_jobs (
    id BIGSERIAL PRIMARY KEY,
    shared_content_id BIGINT NOT NULL REFERENCES shared_contents(id) ON DELETE CASCADE,
    status VARCHAR(30) NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_shared_contents_user_created
    ON shared_contents(user_id, created_at DESC);
CREATE INDEX idx_analysis_jobs_status_created
    ON analysis_jobs(status, created_at);

