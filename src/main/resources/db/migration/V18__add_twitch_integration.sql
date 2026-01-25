-- Twitch channel subscriptions
CREATE TABLE twitch_channel_subscriptions (
    id BIGSERIAL PRIMARY KEY,
    chat_id BIGINT NOT NULL,
    twitch_channel_id VARCHAR(50) NOT NULL,
    twitch_login VARCHAR(50) NOT NULL,
    display_name VARCHAR(100),
    avatar_url TEXT,
    eventsub_subscription_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    UNIQUE (chat_id, twitch_channel_id)
);

CREATE INDEX idx_twitch_subscriptions_chat ON twitch_channel_subscriptions(chat_id);
CREATE INDEX idx_twitch_subscriptions_channel ON twitch_channel_subscriptions(twitch_channel_id);

-- Twitch streams
CREATE TABLE twitch_streams (
    id BIGSERIAL PRIMARY KEY,
    subscription_id BIGINT NOT NULL REFERENCES twitch_channel_subscriptions(id) ON DELETE CASCADE,
    twitch_stream_id VARCHAR(50),
    started_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP,
    telegram_message_id BIGINT,
    telegram_chat_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'live', -- live, ended
    current_game VARCHAR(200),
    current_title TEXT,
    viewer_count INTEGER DEFAULT 0
);

CREATE INDEX idx_twitch_streams_subscription ON twitch_streams(subscription_id);
CREATE INDEX idx_twitch_streams_status ON twitch_streams(status);

-- Stream timeline events
CREATE TABLE stream_timeline_events (
    id BIGSERIAL PRIMARY KEY,
    stream_id BIGINT NOT NULL REFERENCES twitch_streams(id) ON DELETE CASCADE,
    event_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    stream_offset_seconds INTEGER NOT NULL,
    game_name VARCHAR(200),
    stream_title TEXT
);

CREATE INDEX idx_timeline_stream ON stream_timeline_events(stream_id);

-- Twitch notification settings
CREATE TABLE twitch_notification_settings (
    chat_id BIGINT PRIMARY KEY,
    message_template TEXT NOT NULL DEFAULT '🔴 {streamer} начал стрим!

{title}

🎮 {game}
👥 {viewers} зрителей
⏱ {duration}',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

COMMENT ON TABLE twitch_channel_subscriptions IS 'Tracks which Twitch channels are subscribed per chat';
COMMENT ON TABLE twitch_streams IS 'Active and historical stream data';
COMMENT ON TABLE stream_timeline_events IS 'Records game/title changes during streams';
COMMENT ON TABLE twitch_notification_settings IS 'Custom notification message templates per chat';
