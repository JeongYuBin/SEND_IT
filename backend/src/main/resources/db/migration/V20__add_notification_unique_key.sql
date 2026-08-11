ALTER TABLE notifications ADD COLUMN unique_key VARCHAR(160);

CREATE UNIQUE INDEX uq_notifications_unique_key
    ON notifications(unique_key)
    WHERE unique_key IS NOT NULL;
