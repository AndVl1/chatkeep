-- Lock settings table
CREATE TABLE lock_settings (
    chat_id BIGINT PRIMARY KEY,
    locks_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    lock_warns BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);

-- Lock exemptions table
CREATE TABLE lock_exemptions (
    id BIGSERIAL PRIMARY KEY,
    chat_id BIGINT NOT NULL,
    lock_type VARCHAR(50), -- NULL means applies to all lock types
    exemption_type VARCHAR(50) NOT NULL, -- USER, BOT, CHANNEL, STICKER_SET, INLINE_BOT
    exemption_value VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    CONSTRAINT uq_lock_exemptions UNIQUE (chat_id, lock_type, exemption_type, exemption_value)
);

CREATE INDEX idx_lock_exemptions_chat_id ON lock_exemptions(chat_id);
CREATE INDEX idx_lock_exemptions_chat_lock_type ON lock_exemptions(chat_id, lock_type);

-- Lock allowlist table
CREATE TABLE lock_allowlist (
    id BIGSERIAL PRIMARY KEY,
    chat_id BIGINT NOT NULL,
    allowlist_type VARCHAR(50) NOT NULL, -- URL, COMMAND, DOMAIN
    pattern VARCHAR(1024) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    CONSTRAINT uq_lock_allowlist UNIQUE (chat_id, allowlist_type, pattern)
);

CREATE INDEX idx_lock_allowlist_chat_id ON lock_allowlist(chat_id);
CREATE INDEX idx_lock_allowlist_chat_type ON lock_allowlist(chat_id, allowlist_type);
