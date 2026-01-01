-- Add message_text column to punishments table for admin action logging
ALTER TABLE punishments
ADD COLUMN message_text TEXT;

COMMENT ON COLUMN punishments.message_text IS 'The text of the message that triggered moderation (when admin replies to a message)';
