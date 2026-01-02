-- Add clean_service_enabled column to moderation_config
-- When enabled, bot will delete service messages (user joined/left notifications)
ALTER TABLE moderation_config
ADD COLUMN clean_service_enabled BOOLEAN DEFAULT false NOT NULL;
