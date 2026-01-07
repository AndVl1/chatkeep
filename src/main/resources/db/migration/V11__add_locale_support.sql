-- Add locale column to chat_settings table
ALTER TABLE chat_settings ADD COLUMN locale VARCHAR(5) NOT NULL DEFAULT 'en';

-- Create user_preferences table
CREATE TABLE IF NOT EXISTS user_preferences (
    user_id BIGINT PRIMARY KEY,
    locale VARCHAR(5) NOT NULL DEFAULT 'en',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_preferences_user_id ON user_preferences(user_id);
