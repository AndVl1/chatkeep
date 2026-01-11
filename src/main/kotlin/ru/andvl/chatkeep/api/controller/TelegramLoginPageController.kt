package ru.andvl.chatkeep.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import ru.andvl.chatkeep.config.BotProperties

@Controller
@Validated
@Tag(name = "Authentication", description = "Telegram Login Widget page for mobile OAuth")
class TelegramLoginPageController(
    private val botProperties: BotProperties
) {

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
        model: Model
    ): String {
        model.addAttribute("botUsername", botProperties.username)
        model.addAttribute("state", state)
        return "telegram-login"
    }
}
