package com.chatkeep.admin.core.network.contract

import com.chatkeep.admin.core.network.ChatResponse
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue

class ChatsContractTest : ContractTestBase() {
    @Test
    fun `deserialize chat list from backend schema`() {
        assumeTrue(fixtureExists("chats_response.json"), "Fixture not generated yet")
        val jsonString = loadFixture("chats_response.json")
        val result = json.decodeFromString<List<ChatResponse>>(jsonString)

        assertTrue(result.isNotEmpty(), "Expected non-empty chat list")
        assertTrue(result.all { it.chatId != 0L }, "All chats should have non-zero IDs")
    }
}
