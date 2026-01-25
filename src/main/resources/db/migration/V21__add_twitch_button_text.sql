-- Add button_text column to twitch_notification_settings for customizable stream link button
ALTER TABLE twitch_notification_settings
    ADD COLUMN button_text VARCHAR(64) DEFAULT '📺 Смотреть';
