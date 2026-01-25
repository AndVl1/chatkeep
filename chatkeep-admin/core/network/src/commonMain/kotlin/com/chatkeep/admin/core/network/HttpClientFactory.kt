package com.chatkeep.admin.core.network

import com.chatkeep.admin.core.common.BuildConfig
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

private val jsonParser = Json {
    prettyPrint = true
    isLenient = true
    ignoreUnknownKeys = true
}

fun createHttpClient(baseUrl: String? = null): HttpClient = HttpClient {
    // Use expectSuccess to throw on non-2xx responses
    // This triggers HttpResponseValidator BEFORE ContentNegotiation tries to parse
    expectSuccess = true

    install(ContentNegotiation) {
        json(jsonParser)
    }

    install(Logging) {
        logger = Logger.DEFAULT
        level = LogLevel.INFO
    }

    install(HttpTimeout) {
        requestTimeoutMillis = 30_000
        connectTimeoutMillis = 10_000
        socketTimeoutMillis = 30_000
    }

    // Handle exceptions thrown by expectSuccess=true
    HttpResponseValidator {
        handleResponseExceptionWithRequest { exception, _ ->
            val clientException = exception as? ClientRequestException ?: return@handleResponseExceptionWithRequest
            val response = clientException.response
            val bodyText = response.bodyAsText()

            // Try to parse as our custom ErrorResponse first
            val errorInfo = try {
                val parsed = jsonParser.decodeFromString<ErrorResponseDto>(bodyText)
                ErrorInfo(parsed.code, parsed.message, parsed.details)
            } catch (e: Exception) {
                // Try to parse as Spring Boot default error response
                try {
                    val springError = jsonParser.decodeFromString<SpringErrorResponse>(bodyText)
                    ErrorInfo(
                        code = springError.status.toString(),
                        message = springError.error ?: "Unknown error"
                    )
                } catch (e2: Exception) {
                    // Fallback to generic error
                    ErrorInfo(
                        code = response.status.value.toString(),
                        message = response.status.description
                    )
                }
            }

            throw ApiException(
                statusCode = response.status,
                errorCode = errorInfo.code,
                errorMessage = errorInfo.message,
                details = errorInfo.details
            )
        }
    }

    val finalBaseUrl = baseUrl ?: BuildConfig.DEFAULT_BASE_URL
    install(DefaultRequest) {
        url(finalBaseUrl)
    }
}

// Helper class for error info
private data class ErrorInfo(
    val code: String,
    val message: String,
    val details: Map<String, String>? = null
)

// Error response DTO matching backend's ErrorResponse
@kotlinx.serialization.Serializable
data class ErrorResponseDto(
    val code: String,
    val message: String,
    val details: Map<String, String>? = null
)

// Spring Boot default error response format
@kotlinx.serialization.Serializable
data class SpringErrorResponse(
    val timestamp: String? = null,
    val status: Int,
    val error: String? = null,
    val message: String? = null,
    val path: String? = null
)

// Custom exception for API errors
class ApiException(
    val statusCode: HttpStatusCode,
    val errorCode: String,
    val errorMessage: String,
    val details: Map<String, String>? = null
) : Exception("API Error [$errorCode]: $errorMessage (HTTP ${statusCode.value})")
