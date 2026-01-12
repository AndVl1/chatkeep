package ru.andvl.chatkeep.api.auth

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class AdminAuthFilter(
    private val jwtService: JwtService
) : OncePerRequestFilter() {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        const val ADMIN_USER_ATTR = "adminUser"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // Skip authentication for login endpoint
        if (request.requestURI == "/api/v1/admin/auth/login") {
            filterChain.doFilter(request, response)
            return
        }

        // Only apply to admin endpoints
        if (!request.requestURI.startsWith("/api/v1/admin/")) {
            filterChain.doFilter(request, response)
            return
        }

        // Extract JWT token from Authorization header
        val authHeader = request.getHeader("Authorization")
        val token = authHeader?.removePrefix("Bearer ")?.trim()

        if (token == null) {
            logger.debug("Missing Authorization header for admin endpoint: ${request.requestURI}")
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing Authorization header")
            return
        }

        // Validate JWT token
        val user = jwtService.validateAndParse(token)
        if (user == null) {
            logger.debug("Invalid JWT token for admin endpoint: ${request.requestURI}")
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token")
            return
        }

        // Set user in request attribute
        request.setAttribute(ADMIN_USER_ATTR, user)

        // Continue filter chain
        filterChain.doFilter(request, response)
    }
}
