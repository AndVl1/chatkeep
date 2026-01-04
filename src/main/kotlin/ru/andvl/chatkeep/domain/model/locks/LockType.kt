package ru.andvl.chatkeep.domain.model.locks

enum class LockType(
    val category: LockCategory,
    val description: String
) {
    // CONTENT types - media and rich content
    PHOTO(LockCategory.CONTENT, "Photo messages"),
    VIDEO(LockCategory.CONTENT, "Video messages"),
    AUDIO(LockCategory.CONTENT, "Audio messages"),
    VOICE(LockCategory.CONTENT, "Voice messages"),
    DOCUMENT(LockCategory.CONTENT, "Document messages"),
    STICKER(LockCategory.CONTENT, "Sticker messages"),
    GIF(LockCategory.CONTENT, "GIF/Animation messages"),
    VIDEONOTE(LockCategory.CONTENT, "Video notes"),
    CONTACT(LockCategory.CONTENT, "Contact messages"),
    LOCATION(LockCategory.CONTENT, "Location messages"),
    VENUE(LockCategory.CONTENT, "Venue messages"),
    DICE(LockCategory.CONTENT, "Dice/game messages"),
    POLL(LockCategory.CONTENT, "Poll messages"),
    GAME(LockCategory.CONTENT, "Game messages"),

    // FORWARD types - forwarded message controls
    FORWARD(LockCategory.FORWARD, "All forwarded messages"),
    FORWARDUSER(LockCategory.FORWARD, "Forwarded from users"),
    FORWARDCHANNEL(LockCategory.FORWARD, "Forwarded from channels"),
    FORWARDBOT(LockCategory.FORWARD, "Forwarded from bots"),

    // URL types - link and button controls
    URL(LockCategory.URL, "Messages with URLs"),
    BUTTON(LockCategory.URL, "Messages with inline buttons"),
    INVITE(LockCategory.URL, "Telegram invite links"),

    // TEXT types - text formatting and content
    TEXT(LockCategory.TEXT, "Text messages"),
    COMMANDS(LockCategory.TEXT, "Bot commands"),
    EMAIL(LockCategory.TEXT, "Email addresses"),
    PHONE(LockCategory.TEXT, "Phone numbers"),
    SPOILER(LockCategory.TEXT, "Spoiler formatted text"),

    // ENTITY types - mentions and special text
    MENTION(LockCategory.ENTITY, "@mentions"),
    HASHTAG(LockCategory.ENTITY, "#hashtags"),
    CASHTAG(LockCategory.ENTITY, "\$cashtags"),
    EMOJIGAME(LockCategory.ENTITY, "Emoji game messages"),
    EMOJI(LockCategory.ENTITY, "Custom emoji"),
    INLINE(LockCategory.ENTITY, "Inline bot results"),

    // OTHER types - special cases and edge cases
    RTLCHAR(LockCategory.OTHER, "Right-to-left characters"),
    ANONCHANNEL(LockCategory.OTHER, "Anonymous channel posts"),
    COMMENT(LockCategory.OTHER, "Comments"),
    ALBUM(LockCategory.OTHER, "Media albums"),
    TOPIC(LockCategory.OTHER, "Topic messages"),
    PREMIUM(LockCategory.OTHER, "Premium-only features"),

    // ADDITIONAL types to reach 47
    CAPTION(LockCategory.TEXT, "Media captions"),
    LINK(LockCategory.URL, "Text links (not inline buttons)"),
    TEXTLINK(LockCategory.URL, "Markdown/HTML text links"),
    LINKPREVIEW(LockCategory.URL, "Link preview embeds"),
    CHANNELPOST(LockCategory.FORWARD, "Channel posts"),
    SIGNATURE(LockCategory.OTHER, "Channel signature"),
    EDIT(LockCategory.OTHER, "Edited messages"),
    SERVICE(LockCategory.OTHER, "Service messages"),
    NEWMEMBERS(LockCategory.OTHER, "New member join messages"),
    LEFTMEMBER(LockCategory.OTHER, "Member left messages"),
    PINNED(LockCategory.OTHER, "Pinned message notifications")
}

enum class LockCategory {
    CONTENT,    // Media and rich content types
    FORWARD,    // Forwarded message types
    URL,        // Links and buttons
    TEXT,       // Text content and formatting
    ENTITY,     // Mentions and special entities
    OTHER       // Service messages and special cases
}
