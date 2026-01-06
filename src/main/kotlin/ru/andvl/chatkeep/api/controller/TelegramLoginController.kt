package ru.andvl.chatkeep.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.andvl.chatkeep.api.auth.JwtService
import ru.andvl.chatkeep.api.auth.TelegramAuthService
import ru.andvl.chatkeep.api.dto.ErrorResponse
import ru.andvl.chatkeep.api.dto.TelegramLoginRequest
import ru.andvl.chatkeep.api.dto.TelegramUserResponse
import ru.andvl.chatkeep.api.dto.TokenResponse
import java.time.Instant

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Telegram Login Widget authentication")
class TelegramLoginController(
    private val telegramAuthService: TelegramAuthService,
    private val jwtService: JwtService,
    @Value("\${jwt.expiration-hours:24}") private val expirationHours: Long
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @PostMapping("/telegram-login")
    @Operation(
        summary = "Authenticate with Telegram Login Widget",
        description = "Validates Telegram Login Widget data and returns a JWT token"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Successfully authenticated"),
        ApiResponse(responseCode = "400", description = "Invalid request data"),
        ApiResponse(responseCode = "401", description = "Invalid Telegram authentication")
    )
    fun login(@Valid @RequestBody request: TelegramLoginRequest): ResponseEntity<Any> {
        logger.debug("Processing Telegram login for user ${request.id}")

        // Build data map for hash validation
        val dataMap = buildMap {
            put("id", request.id.toString())
            put("first_name", request.firstName)
            request.lastName?.let { put("last_name", it) }
            request.username?.let { put("username", it) }
            request.photoUrl?.let { put("photo_url", it) }
            put("auth_date", request.authDate.toString())
            put("hash", request.hash)
        }

        // Validate hash
        if (!telegramAuthService.validateLoginWidgetHash(dataMap)) {
            logger.warn("Invalid Telegram Login Widget hash for user ${request.id}")
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ErrorResponse(
                    code = "UNAUTHORIZED",
                    message = "Invalid Telegram authentication"
                )
            )
        }

        // Check auth_date expiry (1 hour)
        val now = Instant.now().epochSecond
        if (now - request.authDate > 3600) {
            logger.debug("Expired auth_date for user ${request.id}: ${request.authDate}")
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ErrorResponse(
                    code = "EXPIRED",
                    message = "Authentication data expired"
                )
            )
        }

        // Create TelegramUser object
        val telegramUser = TelegramAuthService.TelegramUser(
            id = request.id,
            firstName = request.firstName,
            lastName = request.lastName,
            username = request.username,
            photoUrl = request.photoUrl,
            authDate = request.authDate
        )

        // Generate JWT token
        val token = jwtService.generateToken(telegramUser)

        logger.info("Successfully authenticated user ${request.id} via Telegram Login Widget")

        return ResponseEntity.ok(
            TokenResponse(
                token = token,
                expiresIn = expirationHours * 60 * 60, // in seconds
                user = TelegramUserResponse(
                    id = telegramUser.id,
                    firstName = telegramUser.firstName,
                    lastName = telegramUser.lastName,
                    username = telegramUser.username,
                    photoUrl = telegramUser.photoUrl
                )
            )
        )
    }
}
