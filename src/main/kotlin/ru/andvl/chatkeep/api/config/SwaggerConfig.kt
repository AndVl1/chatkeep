package ru.andvl.chatkeep.api.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {

    @Bean
    fun openAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("Chatkeep Mini App API")
                    .description("REST API for Telegram Mini App to manage chat settings")
                    .version("1.0.0")
            )
            .components(
                Components()
                    .addSecuritySchemes(
                        "TelegramAuth",
                        SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("tma")
                            .description("Telegram Mini App initData authentication. Format: `tma {initDataRaw}`")
                    )
            )
    }
}
