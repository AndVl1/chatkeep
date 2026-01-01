-- Warnings table
CREATE TABLE warnings (
    id BIGSERIAL PRIMARY KEY,
    chat_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    issued_by_id BIGINT NOT NULL,
    reason VARCHAR(500),
    created_at TIMESTAMP DEFAULT NOW() NOT NULL,
    expires_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_warnings_chat_user ON warnings(chat_id, user_id);
CREATE INDEX idx_warnings_expires_at ON warnings(expires_at);

-- Punishments table
CREATE TABLE punishments (
    id BIGSERIAL PRIMARY KEY,
    chat_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    issued_by_id BIGINT NOT NULL,
    punishment_type VARCHAR(20) NOT NULL, -- NOTHING, WARN, MUTE, BAN, KICK
    duration_seconds BIGINT,
    reason VARCHAR(500),
    source VARCHAR(20) NOT NULL, -- MANUAL, BLOCKLIST, THRESHOLD
    created_at TIMESTAMP DEFAULT NOW() NOT NULL
);

CREATE INDEX idx_punishments_chat_id ON punishments(chat_id);
CREATE INDEX idx_punishments_user_id ON punishments(user_id);
CREATE INDEX idx_punishments_created_at ON punishments(created_at);

-- Moderation config table
CREATE TABLE moderation_config (
    id BIGSERIAL PRIMARY KEY,
    chat_id BIGINT UNIQUE NOT NULL,
    max_warnings INT DEFAULT 3 NOT NULL,
    warning_ttl_hours INT DEFAULT 24 NOT NULL,
    threshold_action VARCHAR(20) DEFAULT 'MUTE' NOT NULL, -- NOTHING, WARN, MUTE, BAN, KICK
    threshold_duration_hours INT DEFAULT 24,
    default_blocklist_action VARCHAR(20) DEFAULT 'WARN' NOT NULL,
    created_at TIMESTAMP DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP DEFAULT NOW() NOT NULL
);

CREATE INDEX idx_moderation_config_chat_id ON moderation_config(chat_id);

-- Blocklist patterns table
CREATE TABLE blocklist_patterns (
    id BIGSERIAL PRIMARY KEY,
    chat_id BIGINT, -- NULL = global pattern
    pattern VARCHAR(500) NOT NULL,
    match_type VARCHAR(20) NOT NULL, -- EXACT, WILDCARD
    action VARCHAR(20) NOT NULL, -- NOTHING, WARN, MUTE, BAN, KICK
    action_duration_hours INT,
    severity INT DEFAULT 0 NOT NULL,
    created_at TIMESTAMP DEFAULT NOW() NOT NULL
);

CREATE INDEX idx_blocklist_patterns_chat_id ON blocklist_patterns(chat_id);

-- Admin sessions table
CREATE TABLE admin_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE NOT NULL,
    connected_chat_id BIGINT NOT NULL,
    connected_chat_title VARCHAR(255),
    created_at TIMESTAMP DEFAULT NOW() NOT NULL
);

CREATE INDEX idx_admin_sessions_user_id ON admin_sessions(user_id);

-- Admin cache table
CREATE TABLE admin_cache (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    chat_id BIGINT NOT NULL,
    is_admin BOOLEAN NOT NULL,
    cached_at TIMESTAMP DEFAULT NOW() NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_admin_cache_user_chat UNIQUE (user_id, chat_id)
);

CREATE INDEX idx_admin_cache_expires_at ON admin_cache(expires_at);
CREATE INDEX idx_admin_cache_user_chat ON admin_cache(user_id, chat_id);
