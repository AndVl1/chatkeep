package ru.andvl.chatkeep.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import ru.andvl.chatkeep.config.AdminBotProperties
import ru.andvl.chatkeep.config.BotProperties

@Controller
@Validated
@Tag(name = "Authentication", description = "Telegram Login Widget page for mobile OAuth")
class TelegramLoginPageController(
    private val botProperties: BotProperties,
    private val adminBotProperties: AdminBotProperties
) {
    private val logger = LoggerFactory.getLogger(TelegramLoginPageController::class.java)

    @GetMapping("/auth/telegram-login")
    @Operation(
        summary = "Telegram Login Widget page",
        description = "Renders the Telegram Login Widget page that redirects to mobile deeplink after authentication"
    )
    fun telegramLoginPage(
        @Parameter(description = "CSRF state parameter for OAuth flow")
        @RequestParam
        @NotBlank(message = "state parameter is required")
        @Size(min = 1, max = 256, message = "state must be between 1 and 256 characters")
        state: String,
        model: Model,
        request: HttpServletRequest
    ): String {
        // Determine which bot to use based on the host
        val host = request.getHeader("Host") ?: ""
        logger.info("Telegram Login page requested. Host: '$host', State: '$state'")
        logger.info("Bot configuration - Main: '${botProperties.username}', Admin: '${adminBotProperties.username}'")

        val botUsername = if (host.startsWith("admin.")) {
            // For admin subdomain, use admin bot if configured, otherwise log warning and fall back
            if (adminBotProperties.username.isNotEmpty()) {
                logger.info("Using admin bot for admin subdomain")
                adminBotProperties.username
            } else {
                logger.warn("Admin subdomain requested but TELEGRAM_ADMINBOT_USERNAME not configured! Falling back to main bot. Please set TELEGRAM_ADMINBOT_USERNAME environment variable.")
                botProperties.username
            }
        } else {
            logger.info("Using main bot for regular domain")
            botProperties.username
        }

        logger.info("Selected bot username: '$botUsername'")

        if (botUsername.isEmpty()) {
            logger.error("Bot username is empty! Check TELEGRAM_BOT_USERNAME or TELEGRAM_ADMINBOT_USERNAME configuration.")
        }

        // Determine if this is test environment for deep link routing
        val isTestEnv = host.contains("chatmodtest")
        logger.info("Environment: ${if (isTestEnv) "TEST" else "PRODUCTION"}")

        model.addAttribute("botUsername", botUsername)
        model.addAttribute("state", state)
        model.addAttribute("isTestEnv", isTestEnv)
        return "telegram-login"
    }
}
