ALTER TABLE users
    ALTER COLUMN password_hash DROP NOT NULL;

ALTER TABLE users
    ADD COLUMN auth_provider VARCHAR(32) NOT NULL DEFAULT 'LOCAL',
    ADD COLUMN provider_subject VARCHAR(255);

UPDATE users
SET auth_provider = 'LOCAL'
WHERE auth_provider IS NULL OR auth_provider = '';

CREATE UNIQUE INDEX uk_users_provider_subject
    ON users (auth_provider, provider_subject)
    WHERE provider_subject IS NOT NULL;
