package ru.andvl.chatkeep.api.auth

import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Date
import javax.crypto.SecretKey

@Service
class JwtService(
    @Value("\${jwt.secret}") private val jwtSecret: String,
    @Value("\${jwt.expiration-hours:24}") private val expirationHours: Long
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private lateinit var secretKey: SecretKey

    @PostConstruct
    fun init() {
        require(jwtSecret.isNotBlank()) {
            "JWT_SECRET must be configured. Please set the JWT_SECRET environment variable."
        }
        require(jwtSecret.length >= 32) {
            "JWT_SECRET must be at least 32 characters long (256 bits). Current length: ${jwtSecret.length}"
        }
        secretKey = Keys.hmacShaKeyFor(jwtSecret.toByteArray())
        logger.info("JwtService initialized successfully")
    }

    data class JwtUser(
        val id: Long,
        val firstName: String,
        val lastName: String?,
        val username: String?,
        val photoUrl: String?
    )

    /**
     * Generates a JWT token for the given Telegram user.
     * Token expires in 24 hours by default.
     */
    fun generateToken(user: TelegramAuthService.TelegramUser): String {
        val now = Date()
        val expiration = Date(now.time + expirationHours * 60 * 60 * 1000)

        return Jwts.builder()
            .subject(user.id.toString())
            .claim("firstName", user.firstName)
            .claim("lastName", user.lastName)
            .claim("username", user.username)
            .claim("photoUrl", user.photoUrl)
            .issuedAt(now)
            .expiration(expiration)
            .signWith(secretKey)
            .compact()
    }

    /**
     * Validates and parses a JWT token.
     * Returns null if token is invalid or expired.
     */
    fun validateAndParse(token: String): JwtUser? {
        return try {
            val claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .payload

            parseUser(claims)
        } catch (e: JwtException) {
            logger.debug("Invalid JWT token: ${e.message}")
            null
        } catch (e: Exception) {
            logger.error("Error parsing JWT token: ${e.message}", e)
            null
        }
    }

    private fun parseUser(claims: Claims): JwtUser? {
        return try {
            val id = claims.subject.toLongOrNull() ?: return null
            val firstName = claims["firstName"] as? String ?: return null
            val lastName = claims["lastName"] as? String
            val username = claims["username"] as? String
            val photoUrl = claims["photoUrl"] as? String

            JwtUser(
                id = id,
                firstName = firstName,
                lastName = lastName,
                username = username,
                photoUrl = photoUrl
            )
        } catch (e: Exception) {
            logger.error("Error extracting user from JWT claims: ${e.message}", e)
            null
        }
    }
}
