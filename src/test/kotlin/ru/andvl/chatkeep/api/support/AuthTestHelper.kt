package ru.andvl.chatkeep.api.support

import org.springframework.http.HttpHeaders
import ru.andvl.chatkeep.api.auth.TelegramAuthService
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Helper for creating test authentication headers.
 * Registered as a bean only in ApiTestConfiguration (not a @Component).
 */
class AuthTestHelper(
    private val telegramAuthService: TestTelegramAuthService
) {

    /**
     * Creates a valid Authorization header with TMA auth for the given user.
     * Registers the user in the test auth service.
     */
    fun createValidAuthHeader(user: TelegramAuthService.TelegramUser): String {
        val fakeInitData = buildInitData(user)

        // Register this user in the test service
        telegramAuthService.registerUser(fakeInitData, user)

        return "tma $fakeInitData"
    }

    /**
     * Creates an invalid Authorization header.
     * Does not register any user, so validation will fail.
     */
    fun createInvalidAuthHeader(): String {
        return "tma invalid_data_not_registered"
    }

    /**
     * Creates HttpHeaders with valid authentication.
     */
    fun createAuthHeaders(user: TelegramAuthService.TelegramUser): HttpHeaders {
        val headers = HttpHeaders()
        headers.set(HttpHeaders.AUTHORIZATION, createValidAuthHeader(user))
        return headers
    }

    /**
     * Clears all registered users from the test auth service.
     */
    fun clearUsers() {
        telegramAuthService.clearUsers()
    }

    /**
     * Builds a fake initData string for testing.
     * Each user gets a unique initData based on their ID.
     */
    private fun buildInitData(user: TelegramAuthService.TelegramUser): String {
        val userJson = buildString {
            append("{")
            append("\"id\":${user.id}")
            append(",\"first_name\":\"${user.firstName}\"")
            user.lastName?.let { append(",\"last_name\":\"$it\"") }
            user.username?.let { append(",\"username\":\"$it\"") }
            user.photoUrl?.let { append(",\"photo_url\":\"$it\"") }
            append("}")
        }

        val encodedUser = URLEncoder.encode(userJson, StandardCharsets.UTF_8)

        // Build query string with required parameters
        // Use user ID in hash to make it unique per user
        return "auth_date=${user.authDate}&user=$encodedUser&hash=test_hash_${user.id}"
    }
}

