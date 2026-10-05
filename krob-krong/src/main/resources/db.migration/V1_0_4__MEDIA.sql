CREATE TABLE media_asset (
    id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id    uuid REFERENCES school (id),
    branch_id    uuid,
    uploaded_by  uuid NOT NULL REFERENCES app_user (id),
    kind         text NOT NULL CHECK (kind IN ('image', 'video', 'audio')),
    purpose      text NOT NULL
                 CHECK (purpose IN ('school_logo', 'school_cover', 'branch_photo', 'user_avatar', 'content')),
    visibility   text NOT NULL DEFAULT 'members' CHECK (visibility IN ('public', 'members', 'staff')),
    mime_type    text NOT NULL,
    byte_size    bigint NOT NULL CHECK (byte_size > 0),
    sha256       text CHECK (sha256 ~ '^[0-9a-f]{64}$'),
    storage_key  text NOT NULL UNIQUE,
    status       text NOT NULL DEFAULT 'pending_upload'
                 CHECK (status IN ('pending_upload', 'processing', 'ready', 'failed', 'quarantined', 'deleted')),
    width        integer CHECK (width > 0),
    height       integer CHECK (height > 0),
    duration_ms  integer CHECK (duration_ms > 0),
    created_at   timestamptz NOT NULL DEFAULT now(),
    ready_at     timestamptz,
    deleted_at   timestamptz,
    UNIQUE (school_id, id),
    UNIQUE (uploaded_by, id),
    FOREIGN KEY (school_id, branch_id) REFERENCES branch (school_id, id),
    CHECK (branch_id IS NULL OR school_id IS NOT NULL),
    CHECK (CASE kind
        WHEN 'image' THEN mime_type IN ('image/jpeg', 'image/png', 'image/webp', 'image/avif')
        WHEN 'video' THEN mime_type IN ('video/mp4', 'video/webm', 'video/quicktime')
        WHEN 'audio' THEN mime_type IN ('audio/mpeg', 'audio/mp4', 'audio/aac', 'audio/ogg', 'audio/wav', 'audio/webm')
    END),
    CHECK (byte_size <= CASE kind WHEN 'image' THEN 20971520
                                  WHEN 'audio' THEN 524288000
                                  ELSE 5368709120 END),
    CHECK (purpose = 'content' OR kind = 'image'),
    CHECK ((purpose = 'user_avatar') = (school_id IS NULL)),
    CHECK (purpose NOT IN ('school_logo', 'school_cover') OR (branch_id IS NULL AND visibility = 'public')),
    CHECK (purpose <> 'branch_photo' OR (branch_id IS NOT NULL AND visibility = 'public')),
    CHECK (purpose <> 'user_avatar' OR visibility = 'public'),
    CHECK (status <> 'ready' OR (sha256 IS NOT NULL AND ready_at IS NOT NULL AND CASE kind
        WHEN 'image' THEN width IS NOT NULL AND height IS NOT NULL
        WHEN 'video' THEN width IS NOT NULL AND height IS NOT NULL AND duration_ms IS NOT NULL
        WHEN 'audio' THEN duration_ms IS NOT NULL
    END)),
    CHECK ((status = 'deleted') = (deleted_at IS NOT NULL))
);
CREATE INDEX media_asset_school_idx ON media_asset (school_id, branch_id);

CREATE TABLE media_rendition (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_id      uuid NOT NULL REFERENCES media_asset (id),
    name          text NOT NULL CHECK (name ~ '^[a-z0-9_]{1,40}$'),
    mime_type     text NOT NULL,
    storage_key   text NOT NULL UNIQUE,
    byte_size     bigint NOT NULL CHECK (byte_size > 0),
    width         integer,
    height        integer,
    bitrate_kbps  integer,
    created_at    timestamptz NOT NULL DEFAULT now(),
    UNIQUE (asset_id, name)
);

ALTER TABLE school
    ADD CONSTRAINT school_logo_fk  FOREIGN KEY (id, logo_asset_id)  REFERENCES media_asset (school_id, id),
    ADD CONSTRAINT school_cover_fk FOREIGN KEY (id, cover_asset_id) REFERENCES media_asset (school_id, id);
ALTER TABLE app_user
    ADD CONSTRAINT app_user_avatar_fk FOREIGN KEY (id, avatar_asset_id) REFERENCES media_asset (uploaded_by, id);
