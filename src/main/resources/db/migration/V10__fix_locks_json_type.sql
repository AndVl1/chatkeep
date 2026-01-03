-- Change locks_json from JSONB to TEXT for simpler Spring Data JDBC compatibility
-- The JSON validation will happen at application level

ALTER TABLE lock_settings
    ALTER COLUMN locks_json TYPE TEXT USING locks_json::TEXT;

-- Update default value
ALTER TABLE lock_settings
    ALTER COLUMN locks_json SET DEFAULT '{}';
