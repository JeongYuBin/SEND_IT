-- Early versions copied an SNS/blog caption into places.description.
-- Keep the caption on shared_contents and clear only exact copied values.
UPDATE places p
SET description = NULL,
    updated_at = CURRENT_TIMESTAMP
WHERE p.tourism_content_id IS NULL
  AND p.description IS NOT NULL
  AND EXISTS (
    SELECT 1
    FROM user_saved_places usp
    JOIN user_saved_place_sources usps ON usps.saved_place_id = usp.id
    JOIN shared_contents sc ON sc.id = usps.shared_content_id
    WHERE usp.place_id = p.id
      AND sc.description IS NOT NULL
      AND BTRIM(sc.description) = BTRIM(p.description)
  );
