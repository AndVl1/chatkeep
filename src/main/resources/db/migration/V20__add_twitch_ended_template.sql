-- Add ended message template for Twitch notifications
ALTER TABLE twitch_notification_settings
    ADD COLUMN ended_message_template TEXT DEFAULT '⚫️ {streamer} завершил стрим

{title}

🎮 {game}
⏱ Продолжительность: {duration}';
