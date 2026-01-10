package ru.andvl.chatkeep.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import ru.andvl.chatkeep.api.auth.AdminAuthFilter
import ru.andvl.chatkeep.api.auth.JwtService
import ru.andvl.chatkeep.api.auth.TelegramAuthService
import ru.andvl.chatkeep.api.dto.AdminLoginRequest
import ru.andvl.chatkeep.api.dto.ErrorResponse
import ru.andvl.chatkeep.api.dto.TelegramUserResponse
import ru.andvl.chatkeep.api.dto.TokenResponse
import ru.andvl.chatkeep.api.exception.AccessDeniedException
import ru.andvl.chatkeep.api.exception.UnauthorizedException
import ru.andvl.chatkeep.config.AdminProperties

@RestController
@RequestMapping("/api/v1/admin/auth")
@Tag(name = "Admin - Authentication", description = "Admin authentication endpoints")
class AdminAuthController(
    private val telegramAuthService: TelegramAuthService,
    private val jwtService: JwtService,
    private val adminProperties: AdminProperties
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @PostMapping("/login")
    @Operation(summary = "Admin login with Telegram", description = "Authenticates admin user using Telegram Login Widget")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Login successful"),
        ApiResponse(responseCode = "401", description = "Invalid Telegram data"),
        ApiResponse(responseCode = "403", description = "User not in admin allowlist")
    )
    fun login(@Valid @RequestBody request: AdminLoginRequest): ResponseEntity<Any> {
        // Convert request to map for validation
        val data = mapOf(
            "id" to request.id.toString(),
            "first_name" to request.firstName,
            "last_name" to (request.lastName ?: ""),
            "username" to (request.username ?: ""),
            "photo_url" to (request.photoUrl ?: ""),
            "auth_date" to request.authDate.toString(),
            "hash" to request.hash
        ).filter { it.value.isNotEmpty() }

        // Validate Telegram Login Widget hash
        if (!telegramAuthService.validateLoginWidgetHash(data)) {
            logger.warn("Invalid Telegram login widget hash for user ${request.id}")
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse(code = "INVALID_HASH", message = "Invalid Telegram data"))
        }

        // Check if user is in admin allowlist
        if (!adminProperties.userIds.contains(request.id)) {
            logger.warn("User ${request.id} attempted admin login but not in allowlist")
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse(code = "ACCESS_DENIED", message = "Access denied"))
        }

        // Create TelegramUser for JWT generation
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

        // Create response
        val tokenResponse = TokenResponse(
            token = token,
            expiresIn = 24 * 60 * 60, // 24 hours in seconds
            user = TelegramUserResponse(
                id = telegramUser.id,
                firstName = telegramUser.firstName,
                lastName = telegramUser.lastName,
                username = telegramUser.username,
                photoUrl = telegramUser.photoUrl
            )
        )

        logger.info("Admin user ${request.id} logged in successfully")
        return ResponseEntity.ok(tokenResponse)
    }

    @GetMapping("/me")
    @Operation(summary = "Get current admin user", description = "Returns information about the authenticated admin user")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success"),
        ApiResponse(responseCode = "401", description = "Unauthorized")
    )
    fun getCurrentUser(request: HttpServletRequest): ResponseEntity<TelegramUserResponse> {
        val user = request.getAttribute(AdminAuthFilter.ADMIN_USER_ATTR) as? JwtService.JwtUser
            ?: throw UnauthorizedException("User not authenticated")

        val response = TelegramUserResponse(
            id = user.id,
            firstName = user.firstName,
            lastName = user.lastName,
            username = user.username,
            photoUrl = user.photoUrl
        )

        return ResponseEntity.ok(response)
    }
}
