# Feature 2: Channel Post Auto-replies

## Status: Phase 2 - Exploration

## Task
Auto-send message under each post from linked channel, configurable via /connect mode.

## Requirements
- Send auto-message under each post from linked channel to chat
- Message content: text + optional media + inline buttons with links
- Handle media groups (multiple photos/videos) → single reply
- Configuration per chat via /connect mode
- Each chat has its own auto-reply settings
- Messages don't leak to other chats

## Key Files to Explore
- GroupMessageHandler.kt - message handling pattern
- Channel post detection in KTgBotAPI
- Media group handling
- Database schema for storing config

## Exploration Findings
(To be filled after Phase 2)

## Database Schema (Draft)
- Table: `channel_reply_settings`
  - chat_id (FK to chat_settings)
  - enabled (boolean)
  - reply_text (text)
  - media_file_id (optional)
  - media_type (optional - photo/video/etc)
  - buttons (JSON - array of {text, url})

## Implementation Notes
- Need to detect channel posts forwarded to linked chat
- Media group handling: track media_group_id, only reply once
- Buttons stored as JSON or separate table
