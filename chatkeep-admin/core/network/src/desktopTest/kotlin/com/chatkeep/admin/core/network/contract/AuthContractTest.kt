package com.chatkeep.admin.core.network.contract

import com.chatkeep.admin.core.network.AdminResponse
import com.chatkeep.admin.core.network.LoginResponse
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Assumptions.assumeTrue

class AuthContractTest : ContractTestBase() {
    @Test
    fun `deserialize LoginResponse from backend schema`() {
        assumeTrue(fixtureExists("login_response.json"), "Fixture not generated yet")
        val jsonString = loadFixture("login_response.json")
        val result = json.decodeFromString<LoginResponse>(jsonString)
        assertNotNull(result.token)
        assertNotNull(result.user)
    }

    @Test
    fun `deserialize AdminResponse from backend schema`() {
        assumeTrue(fixtureExists("admin_response.json"), "Fixture not generated yet")
        val jsonString = loadFixture("admin_response.json")
        val result = json.decodeFromString<AdminResponse>(jsonString)
        assertNotNull(result.id)
    }
}
