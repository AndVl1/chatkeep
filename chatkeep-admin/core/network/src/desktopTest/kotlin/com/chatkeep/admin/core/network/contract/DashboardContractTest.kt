package com.chatkeep.admin.core.network.contract

import com.chatkeep.admin.core.network.DashboardResponse
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Assumptions.assumeTrue

class DashboardContractTest : ContractTestBase() {
    @Test
    fun `deserialize DashboardResponse from backend schema`() {
        assumeTrue(fixtureExists("dashboard_response.json"), "Fixture not generated yet")
        val jsonString = loadFixture("dashboard_response.json")
        val result = json.decodeFromString<DashboardResponse>(jsonString)

        assertNotNull(result.serviceStatus)
        assertNotNull(result.deployInfo)
        assertNotNull(result.quickStats)
    }
}
