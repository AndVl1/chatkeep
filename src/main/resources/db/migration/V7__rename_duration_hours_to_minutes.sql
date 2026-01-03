-- Rename action_duration_hours to action_duration_minutes and convert values
-- Hours -> Minutes (multiply by 60)

ALTER TABLE blocklist_patterns
    RENAME COLUMN action_duration_hours TO action_duration_minutes;

-- Convert existing hour values to minutes
UPDATE blocklist_patterns
SET action_duration_minutes = action_duration_minutes * 60
WHERE action_duration_minutes IS NOT NULL;

-- Also update moderation_config threshold_duration_hours to minutes
ALTER TABLE moderation_config
    RENAME COLUMN threshold_duration_hours TO threshold_duration_minutes;

UPDATE moderation_config
SET threshold_duration_minutes = threshold_duration_minutes * 60
WHERE threshold_duration_minutes IS NOT NULL;
