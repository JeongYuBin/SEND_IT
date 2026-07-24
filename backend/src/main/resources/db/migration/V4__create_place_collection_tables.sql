CREATE TABLE places (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    normalized_name VARCHAR(200) NOT NULL,
    category VARCHAR(100),
    address VARCHAR(500),
    road_address VARCHAR(500),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    geography GEOGRAPHY(POINT, 4326)
        GENERATED ALWAYS AS (
            CASE
                WHEN latitude IS NOT NULL AND longitude IS NOT NULL
                THEN ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)::geography
            END
        ) STORED,
    phone VARCHAR(50),
    homepage_url VARCHAR(2048),
    description TEXT,
    primary_image_url VARCHAR(2048),
    data_source VARCHAR(30) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_places_geography ON places USING GIST (geography);
CREATE INDEX idx_places_normalized_name ON places(normalized_name);

CREATE TABLE collections (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    cover_image_url VARCHAR(2048),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_collections_user_name UNIQUE(user_id, name)
);

CREATE TABLE user_saved_places (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    place_id BIGINT NOT NULL REFERENCES places(id),
    shared_content_id BIGINT REFERENCES shared_contents(id) ON DELETE SET NULL,
    collection_id BIGINT REFERENCES collections(id) ON DELETE SET NULL,
    memo VARCHAR(1000),
    visit_status VARCHAR(30) NOT NULL DEFAULT 'WANT_TO_VISIT',
    priority INTEGER NOT NULL DEFAULT 0,
    saved_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_saved_places_user_place UNIQUE(user_id, place_id),
    CONSTRAINT chk_saved_places_priority CHECK(priority BETWEEN 0 AND 5)
);

CREATE INDEX idx_saved_places_user_saved
    ON user_saved_places(user_id, saved_at DESC);
CREATE INDEX idx_saved_places_collection ON user_saved_places(collection_id);

