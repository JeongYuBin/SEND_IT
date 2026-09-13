-- Preserve existing user choices. Backfill only places that have no collection.
CREATE TEMP TABLE collection_backfill ON COMMIT DROP AS
SELECT s.id, s.user_id,
    CASE
      WHEN COALESCE(p.category, '') ~ '(카페|커피|디저트|베이커리|빵집)' THEN '카페'
      WHEN COALESCE(p.category, '') ~ '(숙박|숙소|호텔|펜션|리조트|게스트)' THEN '숙소'
      WHEN COALESCE(p.category, '') ~ '(음식|식당|구이|요리|한식|중식|일식|양식|갈비|치킨|고기|초밥|술집)' THEN '음식점'
      WHEN COALESCE(p.category, '') ~ '(행사|축제|공연)' THEN '행사'
      WHEN COALESCE(p.category, '') ~ '(관광|문화|레포츠|쇼핑|여행|Tourist)' THEN '여행지'
      ELSE COALESCE(NULLIF(trim(p.category), ''), '기타')
    END AS name
FROM user_saved_places s JOIN places p ON p.id = s.place_id
WHERE s.collection_id IS NULL;

INSERT INTO collections(user_id, name)
SELECT DISTINCT user_id, name FROM collection_backfill
ON CONFLICT (user_id, name) DO NOTHING;

UPDATE user_saved_places s SET collection_id = c.id
FROM collection_backfill b JOIN collections c ON c.user_id = b.user_id AND c.name = b.name
WHERE s.id = b.id;

ALTER TABLE user_saved_places ALTER COLUMN collection_id SET NOT NULL;
