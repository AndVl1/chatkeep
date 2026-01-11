package ru.andvl.chatkeep.api.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter

@Configuration
class CorsConfig {

    @Bean
    fun corsFilter(): CorsFilter {
        val config = CorsConfiguration().apply {
            // Allow Telegram Mini App origins
            allowedOriginPatterns = listOf(
                "https://*.telegram.org",
                "https://*.trycloudflare.com",
                "https://*.ngrok-free.dev",
                "http://localhost:*",
                "https://localhost:*"
            )

            allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
            allowedHeaders = listOf("Authorization", "Content-Type")
            allowCredentials = true
            maxAge = 3600L
        }

        val source = UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/api/v1/miniapp/**", config)
            registerCorsConfiguration("/api/v1/auth/**", config)
            registerCorsConfiguration("/api/v1/admin/**", config)
        }

        return CorsFilter(source)
    }
}
