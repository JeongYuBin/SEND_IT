ALTER TABLE email_verifications ADD COLUMN purpose VARCHAR(30);
UPDATE email_verifications SET purpose = 'SIGNUP' WHERE purpose IS NULL;
ALTER TABLE email_verifications ALTER COLUMN purpose SET NOT NULL;
DROP INDEX idx_email_verifications_email_created;
CREATE INDEX idx_email_verifications_email_purpose_created
    ON email_verifications(email, purpose, created_at DESC);
