CREATE TABLE short_urls (
    id              UUID PRIMARY KEY,
    short_code      VARCHAR(16)  NOT NULL,
    original_url    TEXT         NOT NULL,
    status          VARCHAR(32)  NOT NULL,
    safety_status   VARCHAR(32)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_short_urls_short_code UNIQUE (short_code)
);

CREATE INDEX idx_short_urls_status ON short_urls (status);
