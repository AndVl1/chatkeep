package ru.andvl.chatkeep.domain.service.i18n

import org.springframework.context.MessageSource
import org.springframework.stereotype.Service
import java.util.Locale

@Service
class MessageService(private val messageSource: MessageSource) {

    fun get(key: String, locale: String = "en", vararg args: Any): String {
        return messageSource.getMessage(key, args as Array<Any>, Locale.forLanguageTag(locale))
    }

    fun get(key: String, locale: Locale, vararg args: Any): String {
        return messageSource.getMessage(key, args as Array<Any>, locale)
    }
}
