package ru.andvl.chatkeep.api

import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.get

class TestAuthDebug : MiniAppApiTestBase() {

    @Test
    fun `debug auth flow`() {
        val user = testDataFactory.createTelegramUser(id = 999L)
        
        println("Creating auth header for user: $user")
        val authHeader = authTestHelper.createValidAuthHeader(user)
        println("Auth header: $authHeader")

        val result = mockMvc.get("/api/v1/miniapp/chats") {
            header("Authorization", authHeader)
        }.andReturn()

        println("Response status: ${result.response.status}")
        println("Response body: ${result.response.contentAsString}")
        
        if (result.response.status == 401) {
            println("FAILURE: User not authenticated")
        } else {
            println("SUCCESS: User authenticated")
        }
    }
}
