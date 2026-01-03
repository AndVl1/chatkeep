package ru.andvl.chatkeep.domain.service.locks.detectors

import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.abstracts.PossiblyForwardedMessage
import dev.inmo.tgbotapi.types.message.content.*
import dev.inmo.tgbotapi.types.message.textsources.*
import dev.inmo.tgbotapi.types.chat.PreviewUser
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.UserId
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import ru.andvl.chatkeep.domain.service.locks.DetectionContext
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for lock detectors.
 * Tests detection logic for various message types and content.
 */
class LockDetectorsTest {

    private val defaultContext = DetectionContext(chatId = 123L)

    @Nested
    inner class PhotoLockDetectorTest {
        private val detector = PhotoLockDetector()

        @Test
        fun `should return true when content is PhotoContent`() = runTest {
            // Given
            val photoContent = mockk<PhotoContent>()
            val message = mockk<ContentMessage<PhotoContent>>()
            every { message.content } returns photoContent

            // When
            val result = detector.detect(message, defaultContext)

            // Then
            assertTrue(result)
        }

        @Test
        fun `should return false when content is not PhotoContent`() = runTest {
            // Given
            val textContent = mockk<TextContent>()
            val message = mockk<ContentMessage<TextContent>>()
            every { message.content } returns textContent

            // When
            val result = detector.detect(message, defaultContext)

            // Then
            assertFalse(result)
        }

        @Test
        fun `should return false when content is VideoContent`() = runTest {
            // Given
            val videoContent = mockk<VideoContent>()
            val message = mockk<ContentMessage<VideoContent>>()
            every { message.content } returns videoContent

            // When
            val result = detector.detect(message, defaultContext)

            // Then
            assertFalse(result)
        }
    }

    @Nested
    inner class VideoLockDetectorTest {
        private val detector = VideoLockDetector()

        @Test
        fun `should return true when content is VideoContent`() = runTest {
            // Given
            val videoContent = mockk<VideoContent>()
            val message = mockk<ContentMessage<VideoContent>>()
            every { message.content } returns videoContent

            // When
            val result = detector.detect(message, defaultContext)

            // Then
            assertTrue(result)
        }

        @Test
        fun `should return false when content is not VideoContent`() = runTest {
            // Given
            val textContent = mockk<TextContent>()
            val message = mockk<ContentMessage<TextContent>>()
            every { message.content } returns textContent

            // When
            val result = detector.detect(message, defaultContext)

            // Then
            assertFalse(result)
        }

        @Test
        fun `should return false when content is PhotoContent`() = runTest {
            // Given
            val photoContent = mockk<PhotoContent>()
            val message = mockk<ContentMessage<PhotoContent>>()
            every { message.content } returns photoContent

            // When
            val result = detector.detect(message, defaultContext)

            // Then
            assertFalse(result)
        }
    }

    @Nested
    inner class StickerLockDetectorTest {
        private val detector = StickerLockDetector()

        @Test
        fun `should return true when content is StickerContent`() = runTest {
            // Given
            val stickerContent = mockk<StickerContent>()
            val message = mockk<ContentMessage<StickerContent>>()
            every { message.content } returns stickerContent

            // When
            val result = detector.detect(message, defaultContext)

            // Then
            assertTrue(result)
        }

        @Test
        fun `should return false when content is not StickerContent`() = runTest {
            // Given
            val textContent = mockk<TextContent>()
            val message = mockk<ContentMessage<TextContent>>()
            every { message.content } returns textContent

            // When
            val result = detector.detect(message, defaultContext)

            // Then
            assertFalse(result)
        }
    }

    @Nested
    inner class UrlLockDetectorTest {
        private val detector = UrlLockDetector()

        @Test
        fun `should return true when message contains URL and no allowlist configured`() = runTest {
            // Given
            val urlSource = mockk<URLTextSource>()
            every { urlSource.source } returns "https://example.com"

            val textContent = mockk<TextContent>()
            every { textContent.textSources } returns listOf(urlSource)

            val message = mockk<ContentMessage<TextContent>>()
            every { message.content } returns textContent

            val context = DetectionContext(
                chatId = 123L,
                allowlistedUrls = emptySet(),
                allowlistedDomains = emptySet()
            )

            // When
            val result = detector.detect(message, context)

            // Then
            assertTrue(result)
        }

        @Test
        fun `should return false when message contains no URLs`() = runTest {
            // Given
            val regularText = mockk<RegularTextSource>()
            val textContent = mockk<TextContent>()
            every { textContent.textSources } returns listOf(regularText)

            val message = mockk<ContentMessage<TextContent>>()
            every { message.content } returns textContent

            // When
            val result = detector.detect(message, defaultContext)

            // Then
            assertFalse(result)
        }

        @Test
        fun `should return false when URL is in allowlisted URLs`() = runTest {
            // Given
            val urlSource = mockk<URLTextSource>()
            every { urlSource.source } returns "https://example.com/page"

            val textContent = mockk<TextContent>()
            every { textContent.textSources } returns listOf(urlSource)

            val message = mockk<ContentMessage<TextContent>>()
            every { message.content } returns textContent

            val context = DetectionContext(
                chatId = 123L,
                allowlistedUrls = setOf("https://example.com"),
                allowlistedDomains = emptySet()
            )

            // When
            val result = detector.detect(message, context)

            // Then
            assertFalse(result)
        }

        @Test
        fun `should return false when URL domain is in allowlisted domains`() = runTest {
            // Given
            val urlSource = mockk<URLTextSource>()
            every { urlSource.source } returns "https://example.com/some/path"

            val textContent = mockk<TextContent>()
            every { textContent.textSources } returns listOf(urlSource)

            val message = mockk<ContentMessage<TextContent>>()
            every { message.content } returns textContent

            val context = DetectionContext(
                chatId = 123L,
                allowlistedUrls = emptySet(),
                allowlistedDomains = setOf("example.com")
            )

            // When
            val result = detector.detect(message, context)

            // Then
            assertFalse(result)
        }

        @Test
        fun `should return true when URL is not in allowlist`() = runTest {
            // Given
            val urlSource = mockk<URLTextSource>()
            every { urlSource.source } returns "https://spam.com/bad"

            val textContent = mockk<TextContent>()
            every { textContent.textSources } returns listOf(urlSource)

            val message = mockk<ContentMessage<TextContent>>()
            every { message.content } returns textContent

            val context = DetectionContext(
                chatId = 123L,
                allowlistedUrls = setOf("https://allowed.com"),
                allowlistedDomains = setOf("allowed.com")
            )

            // When
            val result = detector.detect(message, context)

            // Then
            assertTrue(result)
        }

        @Test
        fun `should be case-insensitive when checking allowlist`() = runTest {
            // Given
            val urlSource = mockk<URLTextSource>()
            every { urlSource.source } returns "https://EXAMPLE.COM/page"

            val textContent = mockk<TextContent>()
            every { textContent.textSources } returns listOf(urlSource)

            val message = mockk<ContentMessage<TextContent>>()
            every { message.content } returns textContent

            val context = DetectionContext(
                chatId = 123L,
                allowlistedUrls = emptySet(),
                allowlistedDomains = setOf("example.com")
            )

            // When
            val result = detector.detect(message, context)

            // Then
            assertFalse(result)
        }
    }

    @Nested
    inner class CommandsLockDetectorTest {
        private val detector = CommandsLockDetector()

        @Test
        fun `should return true when message contains command and no allowlist configured`() = runTest {
            // Given
            val commandSource = mockk<BotCommandTextSource>()
            every { commandSource.command } returns "/test"

            val textContent = mockk<TextContent>()
            every { textContent.textSources } returns listOf(commandSource)

            val message = mockk<ContentMessage<TextContent>>()
            every { message.content } returns textContent

            val context = DetectionContext(
                chatId = 123L,
                allowlistedCommands = emptySet()
            )

            // When
            val result = detector.detect(message, context)

            // Then
            assertTrue(result)
        }

        @Test
        fun `should return false when message contains no commands`() = runTest {
            // Given
            val regularText = mockk<RegularTextSource>()
            val textContent = mockk<TextContent>()
            every { textContent.textSources } returns listOf(regularText)

            val message = mockk<ContentMessage<TextContent>>()
            every { message.content } returns textContent

            val context = DetectionContext(
                chatId = 123L,
                allowlistedCommands = setOf("start")
            )

            // When
            val result = detector.detect(message, context)

            // Then
            assertFalse(result)
        }

        @Test
        fun `should return false when command is in allowlist`() = runTest {
            // Given
            val commandSource = mockk<BotCommandTextSource>()
            every { commandSource.command } returns "/start"

            val textContent = mockk<TextContent>()
            every { textContent.textSources } returns listOf(commandSource)

            val message = mockk<ContentMessage<TextContent>>()
            every { message.content } returns textContent

            val context = DetectionContext(
                chatId = 123L,
                allowlistedCommands = setOf("start", "help")
            )

            // When
            val result = detector.detect(message, context)

            // Then
            assertFalse(result)
        }

        @Test
        fun `should return true when command is not in allowlist`() = runTest {
            // Given
            val commandSource = mockk<BotCommandTextSource>()
            every { commandSource.command } returns "/ban"

            val textContent = mockk<TextContent>()
            every { textContent.textSources } returns listOf(commandSource)

            val message = mockk<ContentMessage<TextContent>>()
            every { message.content } returns textContent

            val context = DetectionContext(
                chatId = 123L,
                allowlistedCommands = setOf("start", "help")
            )

            // When
            val result = detector.detect(message, context)

            // Then
            assertTrue(result)
        }

        @Test
        fun `should be case-insensitive when checking allowlist`() = runTest {
            // Given
            val commandSource = mockk<BotCommandTextSource>()
            every { commandSource.command } returns "/START"

            val textContent = mockk<TextContent>()
            every { textContent.textSources } returns listOf(commandSource)

            val message = mockk<ContentMessage<TextContent>>()
            every { message.content } returns textContent

            val context = DetectionContext(
                chatId = 123L,
                allowlistedCommands = setOf("start")
            )

            // When
            val result = detector.detect(message, context)

            // Then
            assertFalse(result)
        }

        @Test
        fun `should handle command with leading slash`() = runTest {
            // Given
            val commandSource = mockk<BotCommandTextSource>()
            every { commandSource.command } returns "/help"

            val textContent = mockk<TextContent>()
            every { textContent.textSources } returns listOf(commandSource)

            val message = mockk<ContentMessage<TextContent>>()
            every { message.content } returns textContent

            val context = DetectionContext(
                chatId = 123L,
                allowlistedCommands = setOf("help") // No slash in allowlist
            )

            // When
            val result = detector.detect(message, context)

            // Then
            assertFalse(result)
        }

        @Test
        fun `should handle command with whitespace`() = runTest {
            // Given
            val commandSource = mockk<BotCommandTextSource>()
            every { commandSource.command } returns " /start "

            val textContent = mockk<TextContent>()
            every { textContent.textSources } returns listOf(commandSource)

            val message = mockk<ContentMessage<TextContent>>()
            every { message.content } returns textContent

            val context = DetectionContext(
                chatId = 123L,
                allowlistedCommands = setOf("start")
            )

            // When
            val result = detector.detect(message, context)

            // Then
            assertFalse(result)
        }
    }

    @Nested
    inner class ForwardLockDetectorTest {
        private val detector = ForwardLockDetector()

        @Test
        fun `should return true when message is forwarded`() = runTest {
            // Given
            val forwardInfo = mockk<dev.inmo.tgbotapi.types.message.ForwardInfo>()
            val textContent = mockk<TextContent>()

            // Create a mock that implements both interfaces
            val message = mockk<ContentMessage<TextContent>>(moreInterfaces = arrayOf(PossiblyForwardedMessage::class))
            every { message.content } returns textContent
            every { (message as PossiblyForwardedMessage).forwardInfo } returns forwardInfo

            // When
            val result = detector.detect(message, defaultContext)

            // Then
            assertTrue(result)
        }

        @Test
        fun `should return false when message is not forwarded`() = runTest {
            // Given
            val textContent = mockk<TextContent>()

            // Create a mock that implements both interfaces
            val message = mockk<ContentMessage<TextContent>>(moreInterfaces = arrayOf(PossiblyForwardedMessage::class))
            every { message.content } returns textContent
            every { (message as PossiblyForwardedMessage).forwardInfo } returns null

            // When
            val result = detector.detect(message, defaultContext)

            // Then
            assertFalse(result)
        }

        @Test
        fun `should return false when message does not implement PossiblyForwardedMessage`() = runTest {
            // Given
            val textContent = mockk<TextContent>()
            val message = mockk<ContentMessage<TextContent>>()
            every { message.content } returns textContent

            // When
            val result = detector.detect(message, defaultContext)

            // Then
            assertFalse(result)
        }
    }

    @Nested
    inner class MentionLockDetectorTest {
        private val detector = MentionLockDetector()

        @Test
        fun `should return true when message contains mention`() = runTest {
            // Given
            val mentionSource = mockk<MentionTextSource>()

            val textContent = mockk<TextContent>()
            every { textContent.textSources } returns listOf(mentionSource)

            val message = mockk<ContentMessage<TextContent>>()
            every { message.content } returns textContent

            // When
            val result = detector.detect(message, defaultContext)

            // Then
            assertTrue(result)
        }

        @Test
        fun `should return false when message contains no mentions`() = runTest {
            // Given
            val regularText = mockk<RegularTextSource>()

            val textContent = mockk<TextContent>()
            every { textContent.textSources } returns listOf(regularText)

            val message = mockk<ContentMessage<TextContent>>()
            every { message.content } returns textContent

            // When
            val result = detector.detect(message, defaultContext)

            // Then
            assertFalse(result)
        }

        @Test
        fun `should return true when message contains multiple mentions`() = runTest {
            // Given
            val mention1 = mockk<MentionTextSource>()
            val mention2 = mockk<MentionTextSource>()
            val regularText = mockk<RegularTextSource>()

            val textContent = mockk<TextContent>()
            every { textContent.textSources } returns listOf(mention1, regularText, mention2)

            val message = mockk<ContentMessage<TextContent>>()
            every { message.content } returns textContent

            // When
            val result = detector.detect(message, defaultContext)

            // Then
            assertTrue(result)
        }

        @Test
        fun `should return false for empty text sources`() = runTest {
            // Given
            val textContent = mockk<TextContent>()
            every { textContent.textSources } returns emptyList()

            val message = mockk<ContentMessage<TextContent>>()
            every { message.content } returns textContent

            // When
            val result = detector.detect(message, defaultContext)

            // Then
            assertFalse(result)
        }
    }

    @Nested
    inner class InviteLockDetectorTest {
        private val detector = InviteLockDetector()

        @Test
        fun `should return true when message contains t_me plus invite link`() = runTest {
            // Given
            val urlSource = mockk<URLTextSource>()
            every { urlSource.source } returns "https://t.me/+AbCdEf123"

            val textContent = mockk<TextContent>()
            every { textContent.textSources } returns listOf(urlSource)

            val message = mockk<ContentMessage<TextContent>>()
            every { message.content } returns textContent

            // When
            val result = detector.detect(message, defaultContext)

            // Then
            assertTrue(result)
        }

        @Test
        fun `should return true when message contains t_me joinchat link`() = runTest {
            // Given
            val urlSource = mockk<URLTextSource>()
            every { urlSource.source } returns "https://t.me/joinchat/AbCdEf123"

            val textContent = mockk<TextContent>()
            every { textContent.textSources } returns listOf(urlSource)

            val message = mockk<ContentMessage<TextContent>>()
            every { message.content } returns textContent

            // When
            val result = detector.detect(message, defaultContext)

            // Then
            assertTrue(result)
        }

        @Test
        fun `should return true when message contains telegram_me plus invite link`() = runTest {
            // Given
            val urlSource = mockk<URLTextSource>()
            every { urlSource.source } returns "https://telegram.me/+AbCdEf123"

            val textContent = mockk<TextContent>()
            every { textContent.textSources } returns listOf(urlSource)

            val message = mockk<ContentMessage<TextContent>>()
            every { message.content } returns textContent

            // When
            val result = detector.detect(message, defaultContext)

            // Then
            assertTrue(result)
        }

        @Test
        fun `should return true when message contains telegram_me joinchat link`() = runTest {
            // Given
            val urlSource = mockk<URLTextSource>()
            every { urlSource.source } returns "https://telegram.me/joinchat/AbCdEf123"

            val textContent = mockk<TextContent>()
            every { textContent.textSources } returns listOf(urlSource)

            val message = mockk<ContentMessage<TextContent>>()
            every { message.content } returns textContent

            // When
            val result = detector.detect(message, defaultContext)

            // Then
            assertTrue(result)
        }

        @Test
        fun `should return false when message contains regular t_me link without invite`() = runTest {
            // Given
            val urlSource = mockk<URLTextSource>()
            every { urlSource.source } returns "https://t.me/channel_name"

            val textContent = mockk<TextContent>()
            every { textContent.textSources } returns listOf(urlSource)

            val message = mockk<ContentMessage<TextContent>>()
            every { message.content } returns textContent

            // When
            val result = detector.detect(message, defaultContext)

            // Then
            assertFalse(result)
        }

        @Test
        fun `should return false when message contains no URLs`() = runTest {
            // Given
            val regularText = mockk<RegularTextSource>()

            val textContent = mockk<TextContent>()
            every { textContent.textSources } returns listOf(regularText)

            val message = mockk<ContentMessage<TextContent>>()
            every { message.content } returns textContent

            // When
            val result = detector.detect(message, defaultContext)

            // Then
            assertFalse(result)
        }

        @Test
        fun `should return false when message contains non-Telegram URLs`() = runTest {
            // Given
            val urlSource = mockk<URLTextSource>()
            every { urlSource.source } returns "https://example.com"

            val textContent = mockk<TextContent>()
            every { textContent.textSources } returns listOf(urlSource)

            val message = mockk<ContentMessage<TextContent>>()
            every { message.content } returns textContent

            // When
            val result = detector.detect(message, defaultContext)

            // Then
            assertFalse(result)
        }

        @Test
        fun `should be case-insensitive when detecting invite links`() = runTest {
            // Given
            val urlSource = mockk<URLTextSource>()
            every { urlSource.source } returns "https://T.ME/+AbCdEf123"

            val textContent = mockk<TextContent>()
            every { textContent.textSources } returns listOf(urlSource)

            val message = mockk<ContentMessage<TextContent>>()
            every { message.content } returns textContent

            // When
            val result = detector.detect(message, defaultContext)

            // Then
            assertTrue(result)
        }
    }
}
