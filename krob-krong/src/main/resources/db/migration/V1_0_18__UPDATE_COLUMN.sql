-- 1. Drop columns removed from AnnouncementEntity
ALTER TABLE announcement
    DROP COLUMN IF EXISTS deleted_at,
    DROP COLUMN IF EXISTS deleted_by;

-- 2. Change id from UUID to VARCHAR(255)
--    to match AnnouncementEntity.id (String)
ALTER TABLE announcement
    ALTER COLUMN id TYPE VARCHAR(255)
        USING id::VARCHAR;

-- 3. Add status if it does not exist
ALTER TABLE announcement
    ADD COLUMN IF NOT EXISTS status VARCHAR(255);

-- 4. Add scheduled_at if it does not exist
ALTER TABLE announcement
    ADD COLUMN IF NOT EXISTS scheduled_at TIMESTAMPTZ;

-- 5. Add published_at if it does not exist
ALTER TABLE announcement
    ADD COLUMN IF NOT EXISTS published_at TIMESTAMPTZ;