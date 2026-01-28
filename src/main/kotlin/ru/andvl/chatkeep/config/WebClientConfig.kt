package ru.andvl.chatkeep.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.codec.json.KotlinSerializationJsonDecoder
import org.springframework.http.codec.json.KotlinSerializationJsonEncoder
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import kotlinx.serialization.json.Json

@Configuration
class WebClientConfig {

    @Bean
    fun webClient(): WebClient {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            prettyPrint = false
        }

        val strategies = ExchangeStrategies.builder()
            .codecs { configurer ->
                configurer.defaultCodecs().kotlinSerializationJsonDecoder(
                    KotlinSerializationJsonDecoder(json)
                )
                configurer.defaultCodecs().kotlinSerializationJsonEncoder(
                    KotlinSerializationJsonEncoder(json)
                )
            }
            .build()

        return WebClient.builder()
            .exchangeStrategies(strategies)
            .build()
    }
}
