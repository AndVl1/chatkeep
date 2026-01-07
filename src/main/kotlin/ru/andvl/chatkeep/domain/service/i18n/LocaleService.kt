package ru.andvl.chatkeep.domain.service.i18n

import org.springframework.stereotype.Service
import ru.andvl.chatkeep.domain.model.UserPreferences
import ru.andvl.chatkeep.infrastructure.repository.ChatSettingsRepository
import ru.andvl.chatkeep.infrastructure.repository.UserPreferencesRepository
import java.time.Instant

@Service
class LocaleService(
    private val chatSettingsRepository: ChatSettingsRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    companion object {
        const val DEFAULT_LOCALE = "en"
        val SUPPORTED_LOCALES = setOf("en", "ru")
    }

    fun getChatLocale(chatId: Long): String {
        return chatSettingsRepository.findByChatId(chatId)
            ?.locale
            ?: DEFAULT_LOCALE
    }

    fun getUserLocale(userId: Long): String {
        return userPreferencesRepository.findById(userId)
            .map { it.locale }
            .orElse(DEFAULT_LOCALE)
    }

    fun setUserLocale(userId: Long, locale: String): String {
        require(locale in SUPPORTED_LOCALES) { "Unsupported locale: $locale. Supported: $SUPPORTED_LOCALES" }
        val now = Instant.now()
        val prefs = userPreferencesRepository.findById(userId)
            .map { it.copy(locale = locale, updatedAt = now) }
            .orElse(UserPreferences(userId = userId, locale = locale, createdAt = now, updatedAt = now))
        userPreferencesRepository.save(prefs)
        return locale
    }
}
