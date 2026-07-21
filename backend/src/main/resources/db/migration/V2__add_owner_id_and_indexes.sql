ALTER TABLE short_urls
    ADD COLUMN IF NOT EXISTS owner_id UUID NULL;

CREATE INDEX IF NOT EXISTS idx_short_urls_created_at ON short_urls (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_short_urls_safety_status ON short_urls (safety_status);
CREATE INDEX IF NOT EXISTS idx_short_urls_owner_id ON short_urls (owner_id);
