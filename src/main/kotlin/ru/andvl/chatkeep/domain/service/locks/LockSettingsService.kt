package ru.andvl.chatkeep.domain.service.locks

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import tools.jackson.core.type.TypeReference
import ru.andvl.chatkeep.domain.model.locks.*
import ru.andvl.chatkeep.infrastructure.repository.locks.LockAllowlistRepository
import ru.andvl.chatkeep.infrastructure.repository.locks.LockExemptionRepository
import ru.andvl.chatkeep.infrastructure.repository.locks.LockSettingsRepository
import java.time.Instant

@Service
class LockSettingsService(
    private val lockSettingsRepository: LockSettingsRepository,
    private val lockExemptionRepository: LockExemptionRepository,
    private val lockAllowlistRepository: LockAllowlistRepository,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    // Get lock for specific type
    fun getLock(chatId: Long, lockType: LockType): LockConfig? {
        val settings = lockSettingsRepository.findByChatId(chatId) ?: return null
        val locks = parseLocksJson(settings.locksJson)
        return locks[lockType.name]
    }

    // Set lock for specific type
    @Transactional
    fun setLock(chatId: Long, lockType: LockType, locked: Boolean, reason: String? = null) {
        val settings = lockSettingsRepository.findByChatId(chatId) ?: LockSettings(chatId = chatId)
        val locks = parseLocksJson(settings.locksJson)

        if (locked) {
            locks[lockType.name] = LockConfig(locked = true, reason = reason)
        } else {
            locks.remove(lockType.name)
        }

        val updated = settings.copy(
            locksJson = serializeLocksJson(locks),
            updatedAt = Instant.now()
        )
        lockSettingsRepository.save(updated)
        logger.info("Lock ${lockType.name} ${if (locked) "enabled" else "disabled"} for chat $chatId")
    }

    // Get all locks for chat
    fun getAllLocks(chatId: Long): Map<LockType, LockConfig> {
        val settings = lockSettingsRepository.findByChatId(chatId) ?: return emptyMap()
        val locks = parseLocksJson(settings.locksJson)
        return locks.mapNotNull { (key, value) ->
            try {
                LockType.valueOf(key) to value
            } catch (e: IllegalArgumentException) {
                logger.warn("Unknown lock type: $key")
                null
            }
        }.toMap()
    }

    // Lock warns
    fun isLockWarnsEnabled(chatId: Long): Boolean {
        return lockSettingsRepository.findByChatId(chatId)?.lockWarns ?: false
    }

    @Transactional
    fun setLockWarns(chatId: Long, enabled: Boolean) {
        val settings = lockSettingsRepository.findByChatId(chatId) ?: LockSettings(chatId = chatId)
        val updated = settings.copy(
            lockWarns = enabled,
            updatedAt = Instant.now()
        )
        lockSettingsRepository.save(updated)
        logger.info("Lock warns ${if (enabled) "enabled" else "disabled"} for chat $chatId")
    }

    // Exemptions
    @Transactional
    fun addExemption(chatId: Long, lockType: LockType?, exemptionType: ExemptionType, value: String) {
        val exemption = LockExemption(
            chatId = chatId,
            lockType = lockType?.name,
            exemptionType = exemptionType.name,
            exemptionValue = value
        )
        lockExemptionRepository.save(exemption)
        logger.info("Added exemption for chat $chatId: type=${exemptionType.name}, value=$value, lockType=${lockType?.name ?: "all"}")
    }

    @Transactional
    fun removeExemption(chatId: Long, exemptionType: ExemptionType, value: String) {
        lockExemptionRepository.deleteByChatIdAndExemptionTypeAndExemptionValue(
            chatId = chatId,
            exemptionType = exemptionType.name,
            exemptionValue = value
        )
        logger.info("Removed exemption for chat $chatId: type=${exemptionType.name}, value=$value")
    }

    fun getExemptions(chatId: Long): List<LockExemption> {
        return lockExemptionRepository.findAllByChatId(chatId)
    }

    fun isExempt(chatId: Long, lockType: LockType, exemptionType: ExemptionType, value: String): Boolean {
        val exemptions = lockExemptionRepository.findAllByChatIdAndLockType(chatId, lockType.name)
        val globalExemptions = lockExemptionRepository.findAllByChatIdAndLockType(chatId, null)

        val allExemptions = exemptions + globalExemptions
        return allExemptions.any {
            it.exemptionType == exemptionType.name && it.exemptionValue == value
        }
    }

    // Allowlist
    @Transactional
    fun addToAllowlist(chatId: Long, type: AllowlistType, pattern: String) {
        val allowlist = LockAllowlist(
            chatId = chatId,
            allowlistType = type.name,
            pattern = pattern
        )
        lockAllowlistRepository.save(allowlist)
        logger.info("Added to allowlist for chat $chatId: type=${type.name}, pattern=$pattern")
    }

    @Transactional
    fun removeFromAllowlist(chatId: Long, type: AllowlistType, pattern: String) {
        lockAllowlistRepository.deleteByChatIdAndAllowlistTypeAndPattern(
            chatId = chatId,
            allowlistType = type.name,
            pattern = pattern
        )
        logger.info("Removed from allowlist for chat $chatId: type=${type.name}, pattern=$pattern")
    }

    fun getAllowlist(chatId: Long, type: AllowlistType): List<String> {
        return lockAllowlistRepository.findAllByChatIdAndAllowlistType(chatId, type.name)
            .map { it.pattern }
    }

    // JSON parsing helpers
    private fun parseLocksJson(json: String?): MutableMap<String, LockConfig> {
        if (json.isNullOrBlank()) return mutableMapOf()
        return try {
            objectMapper.readValue(json, object : TypeReference<MutableMap<String, LockConfig>>() {})
        } catch (e: Exception) {
            logger.error("Failed to parse locks JSON: ${e.message}", e)
            mutableMapOf()
        }
    }

    private fun serializeLocksJson(locks: Map<String, LockConfig>): String {
        return if (locks.isEmpty()) {
            null
        } else {
            objectMapper.writeValueAsString(locks)
        } ?: ""
    }
}
