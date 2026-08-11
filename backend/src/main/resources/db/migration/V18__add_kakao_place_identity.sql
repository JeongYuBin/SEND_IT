ALTER TABLE places
    ADD COLUMN kakao_place_id VARCHAR(50),
    ADD COLUMN kakao_place_url VARCHAR(2048);

CREATE UNIQUE INDEX uk_places_kakao_place_id
    ON places(kakao_place_id)
    WHERE kakao_place_id IS NOT NULL;
