package ru.andvl.chatkeep.bot.util

import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.UserId
import dev.inmo.tgbotapi.types.chat.PreviewUser
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.abstracts.FromUserMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.message.textsources.RegularTextSource
import dev.inmo.tgbotapi.types.message.textsources.TextMentionTextSource
import dev.inmo.tgbotapi.types.message.textsources.TextSource
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Unit tests for MentionExtractor.
 * Tests user ID extraction and argument parsing for moderation commands.
 */
class MentionExtractorTest {

    @Nested
    inner class ExtractArgumentsTest {

        @Test
        fun `extractArguments parses simple command`() {
            // /mute 123456 2h spam
            val result = MentionExtractor.extractArguments("/mute 123456 2h spam")

            assertEquals(listOf("123456", "2h", "spam"), result)
        }

        @Test
        fun `extractArguments handles command with bot username`() {
            // /mute@botname 123456 2h spam
            val result = MentionExtractor.extractArguments("/mute@chatkeep_bot 123456 2h spam")

            assertEquals(listOf("123456", "2h", "spam"), result)
        }

        @Test
        fun `extractArguments handles multiple spaces`() {
            // /mute  123456   2h  spam (multiple spaces)
            val result = MentionExtractor.extractArguments("/mute  123456   2h  spam")

            assertEquals(listOf("123456", "2h", "spam"), result)
        }

        @Test
        fun `extractArguments returns empty list for command only`() {
            val result = MentionExtractor.extractArguments("/mute")

            assertEquals(emptyList(), result)
        }

        @Test
        fun `extractArguments returns empty list for command with bot username only`() {
            val result = MentionExtractor.extractArguments("/mute@chatkeep_bot")

            assertEquals(emptyList(), result)
        }

        @Test
        fun `extractArguments handles empty string`() {
            val result = MentionExtractor.extractArguments("")

            assertEquals(emptyList(), result)
        }

        @Test
        fun `extractArguments handles whitespace only`() {
            val result = MentionExtractor.extractArguments("   ")

            assertEquals(emptyList(), result)
        }

        @Test
        fun `extractArguments preserves argument with reason text`() {
            val result = MentionExtractor.extractArguments("/mute 123456 2h This is a reason with multiple words")

            assertEquals(listOf("123456", "2h", "This", "is", "a", "reason", "with", "multiple", "words"), result)
        }
    }

    @Nested
    inner class ExtractUserIdFromReplyTest {

        @Test
        fun `extractUserId returns user ID from reply message`() {
            // Given
            val targetUserId = 123456789L
            val message = createMessageWithReply(targetUserId)

            // When
            val result = MentionExtractor.extractUserId(message)

            // Then
            assertEquals(targetUserId, result)
        }

        @Test
        fun `extractUserAndArgs returns REPLY source when user ID from reply`() {
            // Given
            val targetUserId = 123456789L
            val message = createMessageWithReply(targetUserId, text = "/mute 2h spam reason")

            // When
            val result = MentionExtractor.extractUserAndArgs(message)

            // Then
            assertEquals(targetUserId, result.userId)
            assertEquals(UserIdSource.REPLY, result.source)
            // When replying, all text args are preserved (no user ID in text to skip)
            assertEquals(listOf("2h", "spam", "reason"), result.arguments)
        }
    }

    @Nested
    inner class ExtractUserIdFromTextMentionTest {

        @Test
        fun `extractUserId returns user ID from TextMentionTextSource`() {
            // Given
            val targetUserId = 987654321L
            val message = createMessageWithTextMention(targetUserId, "/mute @User 2h spam")

            // When
            val result = MentionExtractor.extractUserId(message)

            // Then
            assertEquals(targetUserId, result)
        }

        @Test
        fun `extractUserAndArgs returns TEXT_MENTION source when user ID from mention`() {
            // Given
            val targetUserId = 987654321L
            val message = createMessageWithTextMention(targetUserId, "/mute @User 2h spam")

            // When
            val result = MentionExtractor.extractUserAndArgs(message)

            // Then
            assertEquals(targetUserId, result.userId)
            assertEquals(UserIdSource.TEXT_MENTION, result.source)
            // When using text mention, args[0] (@User) is skipped
            assertEquals(listOf("2h", "spam"), result.arguments)
        }

        @Test
        fun `extractUserAndArgs with TextMention and bot username in command`() {
            // Given: /mute@botname @User 2h
            val targetUserId = 987654321L
            val message = createMessageWithTextMention(targetUserId, "/mute@chatkeep_bot @User 2h")

            // When
            val result = MentionExtractor.extractUserAndArgs(message)

            // Then
            assertEquals(targetUserId, result.userId)
            assertEquals(UserIdSource.TEXT_MENTION, result.source)
            assertEquals(listOf("2h"), result.arguments)
        }
    }

    @Nested
    inner class ExtractUserIdFromNumericArgTest {

        @Test
        fun `extractUserId returns user ID from numeric argument`() {
            // Given
            val targetUserId = 555555555L
            val message = createMessageWithText("/mute $targetUserId 2h spam")

            // When
            val result = MentionExtractor.extractUserId(message)

            // Then
            assertEquals(targetUserId, result)
        }

        @Test
        fun `extractUserAndArgs returns NUMERIC_ARG source when user ID from text`() {
            // Given
            val targetUserId = 555555555L
            val message = createMessageWithText("/mute $targetUserId 2h spam")

            // When
            val result = MentionExtractor.extractUserAndArgs(message)

            // Then
            assertEquals(targetUserId, result.userId)
            assertEquals(UserIdSource.NUMERIC_ARG, result.source)
            // When using numeric ID, args[0] (user ID) is skipped
            assertEquals(listOf("2h", "spam"), result.arguments)
        }

        @Test
        fun `extractUserAndArgs with numeric ID and bot username in command`() {
            // Given: /mute@botname 123456 2h
            val message = createMessageWithText("/mute@chatkeep_bot 123456 2h")

            // When
            val result = MentionExtractor.extractUserAndArgs(message)

            // Then
            assertEquals(123456L, result.userId)
            assertEquals(UserIdSource.NUMERIC_ARG, result.source)
            assertEquals(listOf("2h"), result.arguments)
        }
    }

    @Nested
    inner class ExtractUserIdNotFoundTest {

        @Test
        fun `extractUserId returns null for plain @username`() {
            // Given: /mute @username (plain @username needs API lookup)
            val message = createMessageWithText("/mute @someuser 2h spam")

            // When
            val result = MentionExtractor.extractUserId(message)

            // Then
            assertNull(result)
        }

        @Test
        fun `extractUserAndArgs returns username for API lookup when @username found`() {
            // Given: /mute @someuser 2h spam
            val message = createMessageWithText("/mute @someuser 2h spam")

            // When
            val result = MentionExtractor.extractUserAndArgs(message)

            // Then
            assertNull(result.userId)
            assertEquals(UserIdSource.NOT_FOUND, result.source)
            assertEquals("someuser", result.username) // Username without @
            // @username is skipped, remaining args preserved
            assertEquals(listOf("2h", "spam"), result.arguments)
        }

        @Test
        fun `extractUserAndArgs with @username and bot suffix in command`() {
            // Given: /mute@botname @user 2h
            val message = createMessageWithText("/mute@chatkeep_bot @user 2h")

            // When
            val result = MentionExtractor.extractUserAndArgs(message)

            // Then
            assertNull(result.userId)
            assertEquals(UserIdSource.NOT_FOUND, result.source)
            assertEquals("user", result.username)
            assertEquals(listOf("2h"), result.arguments)
        }

        @Test
        fun `extractUserId returns null for command without arguments`() {
            // Given
            val message = createMessageWithText("/mute")

            // When
            val result = MentionExtractor.extractUserId(message)

            // Then
            assertNull(result)
        }

        @Test
        fun `extractUserAndArgs returns empty args for command without arguments`() {
            // Given
            val message = createMessageWithText("/mute")

            // When
            val result = MentionExtractor.extractUserAndArgs(message)

            // Then
            assertNull(result.userId)
            assertEquals(UserIdSource.NOT_FOUND, result.source)
            assertNull(result.username)
            assertEquals(emptyList(), result.arguments)
        }

        @Test
        fun `extractUserAndArgs returns null username for non-@ first argument`() {
            // Given: /mute invalid_arg - not a username, not a number
            val message = createMessageWithText("/mute invalid_arg 2h")

            // When
            val result = MentionExtractor.extractUserAndArgs(message)

            // Then
            assertNull(result.userId)
            assertEquals(UserIdSource.NOT_FOUND, result.source)
            assertNull(result.username) // Not a @username
            assertEquals(listOf("invalid_arg", "2h"), result.arguments)
        }

        @Test
        fun `extractUserAndArgs handles @ without username`() {
            // Given: /mute @ - just @ symbol, not a valid username
            val message = createMessageWithText("/mute @ 2h")

            // When
            val result = MentionExtractor.extractUserAndArgs(message)

            // Then
            assertNull(result.userId)
            assertEquals(UserIdSource.NOT_FOUND, result.source)
            assertNull(result.username) // @ alone is not a username
            assertEquals(listOf("@", "2h"), result.arguments)
        }
    }

    @Nested
    inner class PriorityTest {

        @Test
        fun `reply takes priority over TextMention`() {
            // Given: Both reply and TextMention present
            val replyUserId = 111111L
            val mentionUserId = 222222L

            val replyUser = mockk<PreviewUser>()
            every { replyUser.id } returns UserId(RawChatId(replyUserId))

            val replyMessage = mockk<FromUserMessage>()
            every { replyMessage.from } returns replyUser

            val mentionUser = mockk<PreviewUser>()
            every { mentionUser.id } returns UserId(RawChatId(mentionUserId))

            val textMention = mockk<TextMentionTextSource>()
            every { textMention.user } returns mentionUser

            val textContent = mockk<TextContent>()
            every { textContent.text } returns "/mute @User 2h"
            every { textContent.textSources } returns listOf(textMention)

            val message = mockk<CommonMessage<TextContent>>()
            every { message.replyTo } returns replyMessage
            every { message.content } returns textContent

            // When
            val result = MentionExtractor.extractUserAndArgs(message)

            // Then - Reply should win
            assertEquals(replyUserId, result.userId)
            assertEquals(UserIdSource.REPLY, result.source)
        }

        @Test
        fun `TextMention takes priority over numeric arg`() {
            // Given: Both TextMention and numeric ID present (unlikely but test priority)
            val mentionUserId = 222222L

            val mentionUser = mockk<PreviewUser>()
            every { mentionUser.id } returns UserId(RawChatId(mentionUserId))

            val textMention = mockk<TextMentionTextSource>()
            every { textMention.user } returns mentionUser

            val textContent = mockk<TextContent>()
            every { textContent.text } returns "/mute @User 333333 2h"  // 333333 is numeric but @User is TextMention
            every { textContent.textSources } returns listOf(textMention)

            val message = mockk<CommonMessage<TextContent>>()
            every { message.replyTo } returns null
            every { message.content } returns textContent

            // When
            val result = MentionExtractor.extractUserAndArgs(message)

            // Then - TextMention should win
            assertEquals(mentionUserId, result.userId)
            assertEquals(UserIdSource.TEXT_MENTION, result.source)
        }
    }

    // Helper methods to create mock messages

    private fun createMessageWithReply(replyToUserId: Long, text: String = "/mute"): CommonMessage<TextContent> {
        val replyUser = mockk<PreviewUser>()
        every { replyUser.id } returns UserId(RawChatId(replyToUserId))

        val replyMessage = mockk<FromUserMessage>()
        every { replyMessage.from } returns replyUser

        val textContent = mockk<TextContent>()
        every { textContent.text } returns text
        every { textContent.textSources } returns listOf<TextSource>()

        val message = mockk<CommonMessage<TextContent>>()
        every { message.replyTo } returns replyMessage
        every { message.content } returns textContent

        return message
    }

    private fun createMessageWithTextMention(mentionUserId: Long, text: String): CommonMessage<TextContent> {
        val mentionUser = mockk<PreviewUser>()
        every { mentionUser.id } returns UserId(RawChatId(mentionUserId))

        val textMention = mockk<TextMentionTextSource>()
        every { textMention.user } returns mentionUser

        val textContent = mockk<TextContent>()
        every { textContent.text } returns text
        every { textContent.textSources } returns listOf(textMention)

        val message = mockk<CommonMessage<TextContent>>()
        every { message.replyTo } returns null
        every { message.content } returns textContent

        return message
    }

    private fun createMessageWithText(text: String): CommonMessage<TextContent> {
        val textContent = mockk<TextContent>()
        every { textContent.text } returns text
        every { textContent.textSources } returns listOf<TextSource>()

        val message = mockk<CommonMessage<TextContent>>()
        every { message.replyTo } returns null
        every { message.content } returns textContent

        return message
    }
}
