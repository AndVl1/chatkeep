CREATE TABLE channel_reply_settings (
    id BIGSERIAL PRIMARY KEY,
    chat_id BIGINT NOT NULL UNIQUE,
    enabled BOOLEAN NOT NULL DEFAULT false,
    reply_text TEXT,
    media_file_id TEXT,
    media_type VARCHAR(20),
    buttons_json TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
