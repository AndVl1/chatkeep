package ru.andvl.chatkeep.api.controller

import tools.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import ru.andvl.chatkeep.config.TwitchProperties
import ru.andvl.chatkeep.domain.service.twitch.TwitchEventSubService
import ru.andvl.chatkeep.domain.service.twitch.TwitchEventSubWebhook
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@RestController
@RequestMapping("/webhooks/twitch")
@Tag(name = "Webhooks - Twitch EventSub")
class TwitchWebhookController(
    private val eventSubService: TwitchEventSubService,
    private val properties: TwitchProperties,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @PostMapping
    fun handleEventSub(
        @RequestBody payload: String,
        @RequestHeader("Twitch-Eventsub-Message-Id") messageId: String?,
        @RequestHeader("Twitch-Eventsub-Message-Timestamp") timestamp: String?,
        @RequestHeader("Twitch-Eventsub-Message-Signature") signature: String?,
        @RequestHeader("Twitch-Eventsub-Message-Type") messageType: String?,
        request: HttpServletRequest
    ): ResponseEntity<*> {
        // Validate timestamp to prevent replay attacks
        if (!isTimestampValid(timestamp)) {
            logger.warn("Invalid or expired Twitch webhook timestamp: $timestamp")
            return ResponseEntity.status(400).body("Invalid or expired timestamp")
        }

        // Verify signature
        if (!verifySignature(messageId, timestamp, payload, signature)) {
            logger.warn("Invalid Twitch webhook signature")
            return ResponseEntity.status(403).body("Invalid signature")
        }

        val webhook = objectMapper.readValue(payload, TwitchEventSubWebhook::class.java)

        when (messageType) {
            "webhook_callback_verification" -> {
                // Challenge verification
                val challenge = webhook.challenge
                logger.info("Twitch webhook verification challenge: $challenge")
                return ResponseEntity.ok(challenge)
            }

            "notification" -> {
                // Process event
                handleNotification(webhook)
                return ResponseEntity.ok().build<Any>()
            }

            "revocation" -> {
                logger.warn("Twitch EventSub subscription revoked: ${webhook.subscription}")
                return ResponseEntity.ok().build<Any>()
            }

            else -> {
                logger.warn("Unknown Twitch webhook message type: $messageType")
                return ResponseEntity.badRequest().body("Unknown message type")
            }
        }
    }

    private fun handleNotification(webhook: TwitchEventSubWebhook) {
        val event = webhook.event ?: return
        val subscription = webhook.subscription

        try {
            when (subscription.type) {
                "stream.online" -> {
                    val broadcasterId = event["broadcaster_user_id"] as? String ?: return
                    val streamId = event["id"] as? String ?: return
                    val startedAt = event["started_at"] as? String ?: return

                    logger.info("Stream online event: broadcaster=$broadcasterId, stream=$streamId")
                    eventSubService.handleStreamOnline(broadcasterId, streamId, startedAt)
                }

                "stream.offline" -> {
                    val broadcasterId = event["broadcaster_user_id"] as? String ?: return

                    logger.info("Stream offline event: broadcaster=$broadcasterId")
                    eventSubService.handleStreamOffline(broadcasterId)
                }

                else -> {
                    logger.warn("Unknown EventSub type: ${subscription.type}")
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to handle Twitch notification", e)
        }
    }

    /**
     * Validate timestamp to prevent replay attacks
     * Twitch recommends rejecting events older than 10 minutes
     */
    private fun isTimestampValid(timestamp: String?): Boolean {
        if (timestamp == null) {
            return false
        }

        return try {
            val eventTime = Instant.parse(timestamp)
            val now = Instant.now()
            val diff = Duration.between(eventTime, now).abs()
            diff.toMinutes() < 10 // Reject events older than 10 minutes
        } catch (e: Exception) {
            logger.warn("Failed to parse Twitch webhook timestamp: $timestamp", e)
            false
        }
    }

    /**
     * Verify Twitch EventSub signature
     * https://dev.twitch.tv/docs/eventsub/handling-webhook-events/#verifying-the-event-message
     */
    private fun verifySignature(messageId: String?, timestamp: String?, body: String, signature: String?): Boolean {
        if (messageId == null || timestamp == null || signature == null) {
            return false
        }

        val message = messageId + timestamp + body
        val hmac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(properties.webhookSecret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256")
        hmac.init(secretKey)

        val hash = hmac.doFinal(message.toByteArray(StandardCharsets.UTF_8))
        val expectedSignature = "sha256=" + hash.joinToString("") { "%02x".format(it) }

        // Use constant-time comparison to prevent timing attacks
        return MessageDigest.isEqual(
            expectedSignature.toByteArray(StandardCharsets.UTF_8),
            signature.toByteArray(StandardCharsets.UTF_8)
        )
    }
}
