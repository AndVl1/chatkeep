package com.chatkeep.admin.core.network

import com.chatkeep.admin.core.common.AppResult
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

interface ApiService {
    suspend fun getItems(): AppResult<List<ApiItem>>
    suspend fun getItemById(id: String): AppResult<ApiItem>
}

class ApiServiceImpl(
    private val client: HttpClient,
    private val baseUrl: String = "https://api.chatkeep.com"
) : ApiService {

    override suspend fun getItems(): AppResult<List<ApiItem>> {
        return try {
            val items = client.get("$baseUrl/items").body<List<ApiItem>>()
            AppResult.Success(items)
        } catch (e: Exception) {
            AppResult.Error("Failed to fetch items: ${e.message}", e)
        }
    }

    override suspend fun getItemById(id: String): AppResult<ApiItem> {
        return try {
            val item = client.get("$baseUrl/items/$id").body<ApiItem>()
            AppResult.Success(item)
        } catch (e: Exception) {
            AppResult.Error("Failed to fetch item: ${e.message}", e)
        }
    }
}

@Serializable
data class ApiItem(
    val id: String,
    val title: String,
    val description: String,
    val timestamp: Long
)
