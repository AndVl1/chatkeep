-- Anti-flood protection settings
CREATE TABLE IF NOT EXISTS antiflood_settings (
    chat_id BIGINT PRIMARY KEY,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    max_messages INT NOT NULL DEFAULT 5,
    time_window_seconds INT NOT NULL DEFAULT 5,
    action VARCHAR(50) NOT NULL DEFAULT 'MUTE',
    action_duration_minutes INT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_antiflood_enabled ON antiflood_settings(enabled);
