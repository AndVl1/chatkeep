package ru.andvl.chatkeep.api.support

import tools.jackson.databind.ObjectMapper
import ru.andvl.chatkeep.api.auth.TelegramAuthService

/**
 * Test implementation of TelegramAuthService that allows tests to register
 * initData -> user mappings without real cryptographic validation.
 */
class TestTelegramAuthService : TelegramAuthService(
    botToken = "test:token",  // Dummy token for tests
    objectMapper = ObjectMapper()
) {

    private val registeredUsers = mutableMapOf<String, TelegramUser>()

    /**
     * Register a user for a specific initData string.
     * When validateAndParse is called with this initData, it will return the user.
     */
    fun registerUser(initData: String, user: TelegramUser) {
        println("Registering user $user for initData: $initData")
        registeredUsers[initData] = user
        println("Registered users count: ${registeredUsers.size}")
    }

    /**
     * Clear all registered users (called between tests).
     */
    fun clearUsers() {
        registeredUsers.clear()
    }

    /**
     * Override validateAndParse to check our test registry first,
     * then fall back to null (no real validation in tests).
     */
    override fun validateAndParse(initDataRaw: String): TelegramUser? {
        println("TestTelegramAuthService.validateAndParse called with: $initDataRaw")
        println("Registered users: ${registeredUsers.keys}")
        val user = registeredUsers[initDataRaw]
        println("Returning user: $user")
        return user
    }
}
