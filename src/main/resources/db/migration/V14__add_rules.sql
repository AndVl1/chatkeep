-- Chat rules
CREATE TABLE IF NOT EXISTS rules (
    chat_id BIGINT PRIMARY KEY,
    rules_text TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_rules_created_at ON rules(created_at);
