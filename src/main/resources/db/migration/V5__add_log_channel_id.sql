-- Add log_channel_id column to moderation_config for real-time log channel notifications
ALTER TABLE moderation_config
ADD COLUMN log_channel_id BIGINT DEFAULT NULL;

COMMENT ON COLUMN moderation_config.log_channel_id IS 'Telegram channel ID where moderation actions are logged in real-time';
