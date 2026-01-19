-- Таблица для хранения медиа как BLOB до загрузки в Telegram
CREATE TABLE media_storage (
    id BIGSERIAL PRIMARY KEY,
    hash VARCHAR(32) NOT NULL UNIQUE,  -- MD5 hash файла
    content BYTEA NOT NULL,            -- Бинарные данные (H2 PostgreSQL mode supports BYTEA)
    mime_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    telegram_file_id TEXT,             -- Заполняется после загрузки в Telegram
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_media_storage_created_at ON media_storage(created_at);

-- Добавляем связь в channel_reply_settings
ALTER TABLE channel_reply_settings
ADD COLUMN media_hash VARCHAR(32);

-- Создаём FK (но hash может быть null если используется legacy media_file_id)
ALTER TABLE channel_reply_settings
ADD CONSTRAINT fk_media_hash
FOREIGN KEY (media_hash) REFERENCES media_storage(hash) ON DELETE SET NULL;
