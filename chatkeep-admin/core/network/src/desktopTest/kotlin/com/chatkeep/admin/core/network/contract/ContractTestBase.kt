package com.chatkeep.admin.core.network.contract

import kotlinx.serialization.json.Json

abstract class ContractTestBase {
    protected val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    protected fun loadFixture(name: String): String {
        val resource = this::class.java.classLoader.getResource("fixtures/$name")
            ?: throw IllegalArgumentException("Fixture not found: $name")
        return resource.readText()
    }

    protected fun fixtureExists(name: String): Boolean {
        return this::class.java.classLoader.getResource("fixtures/$name") != null
    }
}
