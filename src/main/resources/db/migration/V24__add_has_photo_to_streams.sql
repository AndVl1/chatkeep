-- Add has_photo column to twitch_streams table
ALTER TABLE twitch_streams ADD COLUMN has_photo BOOLEAN NOT NULL DEFAULT FALSE;
