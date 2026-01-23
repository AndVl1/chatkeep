-- Chat notes (saved snippets)
CREATE TABLE IF NOT EXISTS notes (
    id BIGSERIAL PRIMARY KEY,
    chat_id BIGINT NOT NULL,
    note_name VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT notes_unique_name UNIQUE (chat_id, note_name)
);

CREATE INDEX idx_notes_chat_id ON notes(chat_id);
CREATE INDEX idx_notes_name ON notes(chat_id, note_name);
