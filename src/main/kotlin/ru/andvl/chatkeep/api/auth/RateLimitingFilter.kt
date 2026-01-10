package ru.andvl.chatkeep.api.auth

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.Refill
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
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Component
@Order(1) // Run before auth filters
class RateLimitingFilter(
    private val objectMapper: ObjectMapper
) : OncePerRequestFilter() {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val buckets = ConcurrentHashMap<String, Bucket>()

    companion object {
        private const val REQUESTS_PER_MINUTE = 100L
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val path = request.requestURI

        // Only apply rate limiting to /api/** endpoints
        if (!path.startsWith("/api/")) {
            filterChain.doFilter(request, response)
            return
        }

        // Get client identifier (IP address)
        val clientId = getClientIpAddress(request)
        val bucket = buckets.computeIfAbsent(clientId) { createBucket() }

        // Try to consume 1 token
        if (bucket.tryConsume(1)) {
            // Allow request
            filterChain.doFilter(request, response)
        } else {
            // Rate limit exceeded
            logger.warn("Rate limit exceeded for client: $clientId on path: $path")
            sendJsonError(
                response,
                429, // 429 Too Many Requests
                "RATE_LIMIT_EXCEEDED",
                "Too many requests. Please try again later."
            )
        }
    }

    private fun createBucket(): Bucket {
        val limit = Bandwidth.classic(
            REQUESTS_PER_MINUTE,
            Refill.intervally(REQUESTS_PER_MINUTE, Duration.ofMinutes(1))
        )
        return Bucket.builder()
            .addLimit(limit)
            .build()
    }

    private fun getClientIpAddress(request: HttpServletRequest): String {
        // Check common proxy headers first
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        if (!xForwardedFor.isNullOrBlank()) {
            return xForwardedFor.split(",").first().trim()
        }

        val xRealIp = request.getHeader("X-Real-IP")
        if (!xRealIp.isNullOrBlank()) {
            return xRealIp
        }

        // Fall back to remote address
        return request.remoteAddr ?: "unknown"
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
