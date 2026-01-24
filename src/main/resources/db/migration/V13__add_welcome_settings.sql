-- Welcome settings for groups
CREATE TABLE IF NOT EXISTS welcome_settings (
    chat_id BIGINT PRIMARY KEY,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    message_text TEXT,
    send_to_chat BOOLEAN NOT NULL DEFAULT TRUE,
    delete_after_seconds INT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_welcome_settings_enabled ON welcome_settings(enabled);
