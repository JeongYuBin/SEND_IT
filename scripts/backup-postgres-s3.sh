#!/usr/bin/env sh
set -eu

: "${POSTGRES_USER:?POSTGRES_USER is required}"
: "${POSTGRES_DB:?POSTGRES_DB is required}"
: "${S3_MEDIA_BUCKET:?S3_MEDIA_BUCKET is required}"

timestamp="$(date -u +%Y%m%d-%H%M%S)"
backup="/tmp/sendit-${timestamp}.dump"
trap 'rm -f "$backup"' EXIT

docker compose exec -T db pg_dump \
  -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc > "$backup"

aws s3 cp "$backup" \
  "s3://${S3_MEDIA_BUCKET}/database-backups/sendit-${timestamp}.dump" \
  --only-show-errors

echo "Database backup uploaded: s3://${S3_MEDIA_BUCKET}/database-backups/sendit-${timestamp}.dump"
