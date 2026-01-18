package ru.andvl.chatkeep.api.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter

@Configuration
@EnableConfigurationProperties(AppDomainsConfig::class)
class CorsConfig(
    private val appDomainsConfig: AppDomainsConfig
) {

    @Bean
    fun corsFilter(): CorsFilter {
        val config = CorsConfiguration().apply {
            // Allow Telegram Mini App origins
            allowedOriginPatterns = buildList {
                // Development and testing origins
                add("https://*.telegram.org")
                add("https://*.trycloudflare.com")
                add("https://*.ngrok-free.dev")
                add("http://localhost:*")
                add("https://localhost:*")

                // Production domains
                appDomainsConfig.main.let { domain ->
                    if (domain != "localhost") {
                        add("https://$domain")
                    }
                }
                appDomainsConfig.miniapp.let { domain ->
                    if (domain != "localhost") {
                        add("https://$domain")
                    }
                }
                appDomainsConfig.admin.let { domain ->
                    if (domain != "localhost") {
                        add("https://$domain")
                    }
                }
            }

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
