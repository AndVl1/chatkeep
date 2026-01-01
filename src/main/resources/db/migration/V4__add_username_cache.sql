-- Username to User ID cache
-- Populated from group messages to enable @username resolution in moderation commands

CREATE TABLE IF NOT EXISTS username_cache (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    user_id BIGINT NOT NULL,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_username_cache_username UNIQUE (username)
);

CREATE INDEX idx_username_cache_username ON username_cache(username);
CREATE INDEX idx_username_cache_user_id ON username_cache(user_id);
