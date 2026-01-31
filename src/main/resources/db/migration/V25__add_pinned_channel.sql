-- Add pinned channel support to twitch_channel_subscriptions table
ALTER TABLE twitch_channel_subscriptions ADD COLUMN is_pinned BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE twitch_channel_subscriptions ADD COLUMN pin_silently BOOLEAN NOT NULL DEFAULT TRUE;
