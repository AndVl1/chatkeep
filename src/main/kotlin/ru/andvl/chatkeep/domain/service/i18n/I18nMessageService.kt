package ru.andvl.chatkeep.domain.service.i18n

import org.slf4j.LoggerFactory
import org.springframework.context.MessageSource
import org.springframework.context.NoSuchMessageException
import org.springframework.stereotype.Service
import java.util.Locale

@Service
class I18nMessageService(private val messageSource: MessageSource) {
    private val logger = LoggerFactory.getLogger(I18nMessageService::class.java)

    fun get(key: String, locale: String = "en", vararg args: Any): String {
        return try {
            messageSource.getMessage(key, args as Array<Any>, Locale.forLanguageTag(locale))
        } catch (e: NoSuchMessageException) {
            logger.warn("Missing translation key: $key for locale: $locale")
            key
        }
    }

    fun get(key: String, locale: Locale, vararg args: Any): String {
        return try {
            messageSource.getMessage(key, args as Array<Any>, locale)
        } catch (e: NoSuchMessageException) {
            logger.warn("Missing translation key: $key for locale: $locale")
            key
        }
    }
}
