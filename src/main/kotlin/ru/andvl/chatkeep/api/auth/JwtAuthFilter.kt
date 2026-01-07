package ru.andvl.chatkeep.api.auth

import tools.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import ru.andvl.chatkeep.api.dto.ErrorResponse

@Component
@Order(2) // Run after TelegramAuthFilter (which has default order 0)
class JwtAuthFilter(
    private val jwtService: JwtService,
    private val objectMapper: ObjectMapper
) : OncePerRequestFilter() {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        const val JWT_USER_ATTR = "jwt.user"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val path = request.requestURI

        // Only apply to Mini App API endpoints
        if (!path.startsWith("/api/v1/miniapp/")) {
            filterChain.doFilter(request, response)
            return
        }

        // If TMA auth already succeeded (TelegramAuthFilter.USER_ATTR is set), skip JWT validation
        if (request.getAttribute(TelegramAuthFilter.USER_ATTR) != null) {
            logger.debug("TMA auth already succeeded for $path, skipping JWT validation")
            filterChain.doFilter(request, response)
            return
        }

        // Extract Authorization header
        val authHeader = request.getHeader("Authorization")
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.debug("Missing or invalid Bearer token for $path")
            sendJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED", "Missing or invalid authorization")
            return
        }

        // Extract JWT token
        val token = authHeader.substring(7).trim()

        // Validate and parse
        val user = jwtService.validateAndParse(token)
        if (user == null) {
            logger.warn("Invalid JWT token for $path")
            sendJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED", "Invalid or expired token")
            return
        }

        // Convert JWT user to TelegramUser for compatibility with existing controllers
        val telegramUser = TelegramAuthService.TelegramUser(
            id = user.id,
            firstName = user.firstName,
            lastName = user.lastName,
            username = user.username,
            photoUrl = user.photoUrl,
            authDate = System.currentTimeMillis() / 1000 // current time for JWT auth
        )

        // Set user in request attribute (using same attribute as TelegramAuthFilter)
        request.setAttribute(TelegramAuthFilter.USER_ATTR, telegramUser)
        logger.debug("Authenticated user ${user.id} via JWT for $path")

        filterChain.doFilter(request, response)
    }

    private fun sendJsonError(
        response: HttpServletResponse,
        status: Int,
        code: String,
        message: String
    ) {
        response.status = status
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        val errorResponse = ErrorResponse(code, message)
        response.writer.write(objectMapper.writeValueAsString(errorResponse))
    }
}
