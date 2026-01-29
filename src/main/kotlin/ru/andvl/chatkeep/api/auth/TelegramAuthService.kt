package ru.andvl.chatkeep.api.auth

import tools.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.math.BigInteger
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.EdECPoint
import java.security.spec.EdECPublicKeySpec
import java.security.spec.NamedParameterSpec
import java.time.Instant
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Service
open class TelegramAuthService(
    @Value("\${telegram.bot.token}") private val botToken: String,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    // Extract bot ID from token (first part before ':')
    // For test tokens without ':', use 0 as a placeholder (signature validation won't be used in tests)
    private val botId: Long = runCatching {
        botToken.split(":").firstOrNull()?.toLongOrNull() ?: 0L
    }.getOrDefault(0L)

    // Telegram's Ed25519 public key for signature validation (production)
    // See: https://core.telegram.org/bots/webapps#validating-data-received-via-the-mini-app
    private val ED25519_PUBLIC_KEY_HEX = "e7bf03a2fa4602af4580703d88dda5bb59f32ed8b02a56c187fe7d34caed242d"

    data class TelegramUser(
        val id: Long,
        val firstName: String,
        val lastName: String?,
        val username: String?,
        val photoUrl: String?,
        val authDate: Long
    )

    /**
     * Validates Telegram Mini App initData using Ed25519 signature (preferred) or HMAC-SHA256 (fallback).
     * See: https://core.telegram.org/bots/webapps#validating-data-received-via-the-mini-app
     */
    open fun validateAndParse(initDataRaw: String): TelegramUser? {
        try {
            logger.debug("Validating initData, length={}, preview={}",
                initDataRaw.length,
                initDataRaw.take(100).replace(Regex("\\s"), " ")
            )

            // Parse query string
            val params = parseQueryString(initDataRaw)

            logger.debug("Extracted parameters: keys={}", params.keys.joinToString(", "))

            // Try Ed25519 signature validation first (new method)
            val signature = params["signature"]
            if (signature != null) {
                logger.debug("Found signature parameter, attempting Ed25519 validation")

                // Build data-check-string for Ed25519 (excludes hash and signature)
                val dataCheckString = buildDataCheckString(params, excludeKeys = setOf("hash", "signature"))

                if (validateSignature(dataCheckString, signature)) {
                    logger.debug("Ed25519 signature validation successful")
                    return extractUserFromParams(params)
                } else {
                    logger.warn("Invalid Ed25519 signature in initData")
                    return null
                }
            }

            // Fallback to HMAC-SHA256 hash validation (legacy method)
            val receivedHash = params["hash"] ?: run {
                logger.debug("Missing both signature and hash in initData")
                return null
            }

            logger.debug("No signature found, falling back to HMAC-SHA256 hash validation")

            // Build data-check-string for hash (excludes hash only)
            val dataCheckString = buildDataCheckString(params, excludeKeys = setOf("hash"))

            // Validate hash
            if (!validateHash(dataCheckString, receivedHash)) {
                logger.warn("Invalid hash in initData")
                return null
            }

            logger.debug("HMAC-SHA256 hash validation successful")
            return extractUserFromParams(params)
        } catch (e: Exception) {
            logger.error("Error validating initData: ${e.message}", e)
            return null
        }
    }

    /**
     * Validates Telegram Login Widget hash using HMAC-SHA256.
     * See: https://core.telegram.org/widgets/login#checking-authorization
     */
    fun validateLoginWidgetHash(data: Map<String, String>): Boolean {
        return validateLoginWidgetHash(data, botToken)
    }

    /**
     * Validates Telegram Login Widget hash using HMAC-SHA256 with a custom bot token.
     * Use this when validating login widget data from a different bot (e.g., admin bot).
     */
    fun validateLoginWidgetHash(data: Map<String, String>, customBotToken: String): Boolean {
        try {
            // Extract hash
            val receivedHash = data["hash"] ?: run {
                logger.debug("Missing hash in login widget data")
                return false
            }

            // Remove hash from data for validation
            val dataCheckString = data
                .filter { it.key != "hash" }
                .toSortedMap()
                .map { "${it.key}=${it.value}" }
                .joinToString("\n")

            // Validate hash using provided bot token
            return validateLoginWidgetHashInternal(dataCheckString, receivedHash, customBotToken)
        } catch (e: Exception) {
            logger.error("Error validating login widget hash: ${e.message}", e)
            return false
        }
    }

    private fun parseQueryString(query: String): Map<String, String> {
        return query.split("&")
            .mapNotNull { pair ->
                val parts = pair.split("=", limit = 2)
                if (parts.size == 2) {
                    val key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8)
                    val value = URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
                    key to value
                } else {
                    null
                }
            }
            .toMap()
    }

    /**
     * Builds the data-check-string for validation by sorting params alphabetically
     * and excluding specified keys (hash, signature).
     */
    private fun buildDataCheckString(params: Map<String, String>, excludeKeys: Set<String>): String {
        return params
            .filter { it.key !in excludeKeys }
            .toSortedMap()
            .map { "${it.key}=${it.value}" }
            .joinToString("\n")
    }

    /**
     * Validates Ed25519 signature using Telegram's public key.
     * Format: {botId}:WebAppData\n{data-check-string}
     */
    private fun validateSignature(dataCheckString: String, signatureB64: String): Boolean {
        try {
            // Build the message to verify: "{botId}:WebAppData\n{data-check-string}"
            val message = "$botId:WebAppData\n$dataCheckString"

            logger.debug(
                "Ed25519 validation - botId={}, message preview={}",
                botId,
                message.take(100).replace("\n", "\\n")
            )

            // Decode base64url signature
            val signatureBytes = Base64.getUrlDecoder().decode(addBase64Padding(signatureB64))

            // Create Ed25519 public key from hex
            val publicKey = createEd25519PublicKey(ED25519_PUBLIC_KEY_HEX)

            // Verify signature
            val sig = Signature.getInstance("Ed25519")
            sig.initVerify(publicKey)
            sig.update(message.toByteArray(StandardCharsets.UTF_8))
            val isValid = sig.verify(signatureBytes)

            if (!isValid) {
                logger.debug(
                    "Ed25519 signature mismatch - signature preview={}",
                    signatureB64.take(16) + "..."
                )
            }

            return isValid
        } catch (e: Exception) {
            logger.error("Error validating Ed25519 signature: ${e.message}", e)
            return false
        }
    }

    /**
     * Creates Ed25519 public key from hex string (compressed point format).
     * Ed25519 uses little-endian encoding with sign bit in MSB of last byte.
     */
    private fun createEd25519PublicKey(hexKey: String): java.security.PublicKey {
        // Decode hex to bytes (32 bytes in little-endian)
        val keyBytes = hexKey.chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()

        // Ed25519 compressed point format (little-endian):
        // - Last byte's MSB contains the sign of x (isXOdd)
        // - Remaining 255 bits are the y-coordinate

        // Get sign bit from MSB of last byte
        val isXOdd = (keyBytes[31].toInt() and 0x80) != 0

        // Clear the sign bit to get pure y-coordinate
        val yBytes = keyBytes.copyOf()
        yBytes[31] = (yBytes[31].toInt() and 0x7F).toByte()

        // Reverse for big-endian (Java BigInteger expects big-endian)
        yBytes.reverse()

        // Create BigInteger from y-coordinate
        val y = BigInteger(1, yBytes)

        // Create EdECPoint
        val point = EdECPoint(isXOdd, y)

        // Create public key spec
        val keySpec = EdECPublicKeySpec(NamedParameterSpec.ED25519, point)

        // Generate public key
        return KeyFactory.getInstance("Ed25519").generatePublic(keySpec)
    }

    /**
     * Adds padding to base64url string if needed.
     */
    private fun addBase64Padding(s: String): String = when (s.length % 4) {
        2 -> "$s=="
        3 -> "$s="
        else -> s
    }

    /**
     * Extracts and validates user data from params.
     */
    private fun extractUserFromParams(params: Map<String, String>): TelegramUser? {
        // Check auth_date expiry (1 hour)
        val authDate = params["auth_date"]?.toLongOrNull() ?: run {
            logger.debug("Missing or invalid auth_date")
            return null
        }

        val now = Instant.now().epochSecond
        if (now - authDate > 3600) {
            logger.debug("Expired auth_date: $authDate")
            return null
        }

        // Parse user data
        val userJson = params["user"] ?: run {
            logger.debug("Missing user data")
            return null
        }

        return parseUser(userJson, authDate)
    }

    private fun validateHash(dataCheckString: String, receivedHash: String): Boolean {
        // Create secret key from bot token
        val secretKey = hmacSha256("WebAppData".toByteArray(), botToken.toByteArray())

        // Calculate expected hash
        val expectedHash = hmacSha256(dataCheckString.toByteArray(), secretKey)
        val expectedHashHex = expectedHash.joinToString("") { "%02x".format(it) }

        // Use constant-time comparison to prevent timing attacks
        val isValid = MessageDigest.isEqual(
            expectedHashHex.toByteArray(StandardCharsets.UTF_8),
            receivedHash.toByteArray(StandardCharsets.UTF_8)
        )

        if (!isValid) {
            logger.debug(
                "Hash mismatch - dataCheckString preview={}, expected={}, received={}",
                dataCheckString.take(50).replace("\n", "\\n"),
                expectedHashHex.take(16) + "...",
                receivedHash.take(16) + "..."
            )
        }

        return isValid
    }

    private fun validateLoginWidgetHashInternal(dataCheckString: String, receivedHash: String, tokenToUse: String = botToken): Boolean {
        // For Login Widget, use bot token directly as key (different from Mini App)
        // IMPORTANT: Use UTF-8 explicitly for consistent hash calculation
        val secretKey = MessageDigest.getInstance("SHA-256").digest(tokenToUse.toByteArray(StandardCharsets.UTF_8))

        // Calculate expected hash
        val expectedHash = hmacSha256(dataCheckString.toByteArray(StandardCharsets.UTF_8), secretKey)
        val expectedHashHex = expectedHash.joinToString("") { "%02x".format(it) }

        // Log bytes for debugging encoding issues (debug only)
        if (logger.isDebugEnabled) {
            val dataBytes = dataCheckString.toByteArray(StandardCharsets.UTF_8)
            logger.debug("Hash validation - dataBytes hex (first 100): {}", dataBytes.take(100).joinToString("") { "%02x".format(it) })
            logger.debug("Hash validation - expected: {}, received: {}", expectedHashHex.take(8) + "...", receivedHash.take(8) + "...")
        }

        // Use constant-time comparison to prevent timing attacks
        val isValid = MessageDigest.isEqual(
            expectedHashHex.toByteArray(StandardCharsets.UTF_8),
            receivedHash.toByteArray(StandardCharsets.UTF_8)
        )

        if (!isValid) {
            logger.warn("Hash mismatch - expected: {}, received: {}", expectedHashHex, receivedHash)
        }

        return isValid
    }

    private fun hmacSha256(data: ByteArray, key: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    private fun parseUser(userJson: String, authDate: Long): TelegramUser? {
        return try {
            val userMap = objectMapper.readValue(userJson, Map::class.java)

            val id = when (val idValue = userMap["id"]) {
                is Number -> idValue.toLong()
                else -> return null
            }

            val firstName = userMap["first_name"] as? String ?: return null
            val lastName = userMap["last_name"] as? String
            val username = userMap["username"] as? String
            val photoUrl = userMap["photo_url"] as? String

            TelegramUser(
                id = id,
                firstName = firstName,
                lastName = lastName,
                username = username,
                photoUrl = photoUrl,
                authDate = authDate
            )
        } catch (e: Exception) {
            logger.error("Error parsing user JSON: ${e.message}", e)
            null
        }
    }
}
