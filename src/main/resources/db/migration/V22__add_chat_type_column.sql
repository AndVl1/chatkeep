-- Add chat_type column to differentiate between groups, supergroups, channels
-- Add with default value 'GROUP' for new inserts
ALTER TABLE chat_settings
ADD COLUMN chat_type VARCHAR(20) DEFAULT 'GROUP';

-- Set values for existing chats based on chat_id pattern
-- Channels and supergroups start with -100 prefix
-- Regular groups are negative but smaller in absolute value
UPDATE chat_settings
SET chat_type = CASE
    WHEN chat_id < -1000000000000 THEN 'SUPERGROUP'
    WHEN chat_id < 0 THEN 'GROUP'
    ELSE 'PRIVATE'
END
WHERE chat_type IS NULL OR chat_type = 'GROUP';

-- Make it NOT NULL (all rows now have values)
ALTER TABLE chat_settings
ALTER COLUMN chat_type SET NOT NULL;
