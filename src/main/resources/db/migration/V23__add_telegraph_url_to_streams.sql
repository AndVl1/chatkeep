-- Add telegraph_url column to twitch_streams table
ALTER TABLE twitch_streams
ADD COLUMN telegraph_url TEXT;

COMMENT ON COLUMN twitch_streams.telegraph_url IS 'Telegraph page URL for full stream timeline (created when caption > 600 chars)';
