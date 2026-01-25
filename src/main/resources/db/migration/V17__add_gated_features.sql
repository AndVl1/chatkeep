-- Gated Features table
CREATE TABLE chat_gated_features (
    chat_id BIGINT NOT NULL,
    feature_key VARCHAR(50) NOT NULL,
    enabled BOOLEAN DEFAULT false,
    enabled_at TIMESTAMP,
    enabled_by BIGINT,
    PRIMARY KEY (chat_id, feature_key)
);

CREATE INDEX idx_gated_features_chat ON chat_gated_features(chat_id);

COMMENT ON TABLE chat_gated_features IS 'Stores which gated features are enabled for each chat';
COMMENT ON COLUMN chat_gated_features.feature_key IS 'Unique feature identifier (e.g. twitch_integration)';
COMMENT ON COLUMN chat_gated_features.enabled_by IS 'Telegram user ID who enabled the feature';
