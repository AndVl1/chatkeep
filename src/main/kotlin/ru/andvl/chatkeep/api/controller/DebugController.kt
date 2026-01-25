package ru.andvl.chatkeep.api.controller

import io.swagger.v3.oas.annotations.Hidden
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.andvl.chatkeep.api.dto.ActionResponse
import ru.andvl.chatkeep.domain.service.twitch.TwitchChannelService

/**
 * Debug endpoints for test environment only.
 * NOT available in production.
 */
@RestController
@RequestMapping("/api/v1/debug")
@Profile("test-env")
@Hidden
class DebugController(
    private val twitchChannelService: TwitchChannelService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @PostMapping("/twitch/resync")
    fun resyncTwitchEventSub(): ResponseEntity<ActionResponse> {
        return try {
            logger.info("[DEBUG] Initiating Twitch EventSub resync")

            val results = twitchChannelService.resyncEventSubSubscriptions()

            val successCount = results.count { it.second }
            val failCount = results.count { !it.second }

            val message = if (results.isEmpty()) {
                "No channels need resyncing - all have EventSub IDs"
            } else {
                val details = results.joinToString(", ") { (login, success) ->
                    "$login: ${if (success) "✓" else "✗"}"
                }
                "Resync completed: $successCount success, $failCount failed. Details: $details"
            }

            logger.info("[DEBUG] Twitch resync completed: $message")

            ResponseEntity.ok(
                ActionResponse(
                    success = failCount == 0,
                    message = message
                )
            )
        } catch (e: Exception) {
            logger.error("[DEBUG] Error during Twitch resync: ${e.message}", e)
            ResponseEntity.ok(
                ActionResponse(
                    success = false,
                    message = "Error during Twitch resync: ${e.message}"
                )
            )
        }
    }
}
