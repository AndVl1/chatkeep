package com.chatkeep.admin.core.network.contract

import com.chatkeep.admin.core.network.TriggerResponse
import com.chatkeep.admin.core.network.WorkflowResponse
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue

class WorkflowsContractTest : ContractTestBase() {
    @Test
    fun `deserialize workflow list from backend schema`() {
        assumeTrue(fixtureExists("workflows_response.json"), "Fixture not generated yet")
        val jsonString = loadFixture("workflows_response.json")
        val result = json.decodeFromString<List<WorkflowResponse>>(jsonString)

        assertTrue(result.isNotEmpty(), "Expected non-empty workflow list")
        result.forEach { workflow ->
            assertNotNull(workflow.id)
            assertNotNull(workflow.name)
            assertNotNull(workflow.filename)
        }
    }

    @Test
    fun `deserialize TriggerResponse from backend schema`() {
        assumeTrue(fixtureExists("trigger_response.json"), "Fixture not generated yet")
        val jsonString = loadFixture("trigger_response.json")
        val result = json.decodeFromString<TriggerResponse>(jsonString)

        assertTrue(result.success)
        assertNotNull(result.message)
        assertNotNull(result.workflowId)
    }

    @Test
    fun `deserialize ActionResponse from backend schema`() {
        assumeTrue(fixtureExists("action_response.json"), "Fixture not generated yet")
        val jsonString = loadFixture("action_response.json")
        val result = json.decodeFromString<com.chatkeep.admin.core.network.ActionResponse>(jsonString)

        assertNotNull(result.message)
    }
}
