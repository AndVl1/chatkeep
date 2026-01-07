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
@Order(1)
class TelegramAuthFilter(
    private val authService: TelegramAuthService,
    private val objectMapper: ObjectMapper
) : OncePerRequestFilter() {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        const val USER_ATTR = "telegram.user"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        println("[FILTER] doFilterInternal called for: ${request.requestURI}")
        val path = request.requestURI

        // Only apply to Mini App API endpoints
        if (!path.startsWith("/api/v1/miniapp/")) {
            filterChain.doFilter(request, response)
            return
        }

        // Extract Authorization header
        val authHeader = request.getHeader("Authorization")

        // If it's a Bearer token, let JWT filter handle it
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            logger.debug("Bearer token detected for $path, skipping TMA validation")
            filterChain.doFilter(request, response)
            return
        }

        if (authHeader == null || !authHeader.startsWith("tma ")) {
            logger.debug("Missing or invalid Authorization header for $path")
            sendJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED", "Missing or invalid authorization")
            return
        }

        // Extract initDataRaw
        val initDataRaw = authHeader.substring(4).trim()

        // Validate and parse
        println("[FILTER TEST] Calling authService.validateAndParse with: ${initDataRaw.take(50)}")
        println("[FILTER TEST] AuthService class: ${authService::class.java.name}")
        val user = authService.validateAndParse(initDataRaw)
        println("[FILTER TEST] Returned user: $user")
        if (user == null) {
            logger.warn("Invalid Telegram auth for $path")
            sendJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED", "Invalid Telegram authentication")
            return
        }

        // Set user in request attribute
        request.setAttribute(USER_ATTR, user)
        logger.debug("Authenticated user ${user.id} for $path")

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
