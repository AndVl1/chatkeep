package com.chatkeep.admin.core.network.contract

import com.chatkeep.admin.core.network.LogsResponse
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue

class LogsContractTest : ContractTestBase() {
    @Test
    fun `deserialize LogsResponse from backend schema`() {
        assumeTrue(fixtureExists("logs_response.json"), "Fixture not generated yet")
        val jsonString = loadFixture("logs_response.json")
        val result = json.decodeFromString<LogsResponse>(jsonString)

        assertNotNull(result.entries)
        assertTrue(result.totalCount >= 0)
        assertNotNull(result.fromTime)
        assertNotNull(result.toTime)

        if (result.entries.isNotEmpty()) {
            val firstEntry = result.entries.first()
            assertNotNull(firstEntry.timestamp)
            assertNotNull(firstEntry.level)
            assertNotNull(firstEntry.logger)
            assertNotNull(firstEntry.message)
        }
    }
}
