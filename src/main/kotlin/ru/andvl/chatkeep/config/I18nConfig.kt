package ru.andvl.chatkeep.config

import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.ResourceBundleMessageSource
import java.nio.charset.StandardCharsets

@Configuration
class I18nConfig {

    @Bean
    fun messageSource(): MessageSource {
        val messageSource = ResourceBundleMessageSource()
        messageSource.setBasename("messages")
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name())
        messageSource.setFallbackToSystemLocale(false)
        return messageSource
    }
}
