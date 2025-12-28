-- Chat settings table
CREATE TABLE chat_settings (
    id BIGSERIAL PRIMARY KEY,
    chat_id BIGINT UNIQUE NOT NULL,
    chat_title VARCHAR(255),
    collection_enabled BOOLEAN DEFAULT true NOT NULL,
    created_at TIMESTAMP DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP DEFAULT NOW() NOT NULL
);

-- Messages table
CREATE TABLE messages (
    id BIGSERIAL PRIMARY KEY,
    telegram_message_id BIGINT NOT NULL,
    chat_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    username VARCHAR(255),
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    text TEXT NOT NULL,
    message_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT NOW() NOT NULL,
    CONSTRAINT uq_chat_message UNIQUE (chat_id, telegram_message_id)
);

-- Indexes for performance
CREATE INDEX idx_messages_chat_id ON messages(chat_id);
CREATE INDEX idx_messages_user_id ON messages(user_id);
CREATE INDEX idx_messages_date ON messages(message_date);
CREATE INDEX idx_chat_settings_chat_id ON chat_settings(chat_id);
