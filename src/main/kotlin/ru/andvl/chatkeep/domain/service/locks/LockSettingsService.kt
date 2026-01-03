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
import ru.andvl.chatkeep.infrastructure.repository.locks.findByChatId
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
        val existingSettings = lockSettingsRepository.findByChatId(chatId)

        val locks = if (existingSettings != null) {
            parseLocksJson(existingSettings.locksJson)
        } else {
            mutableMapOf()
        }

        if (locked) {
            locks[lockType.name] = LockConfig(locked = true, reason = reason)
        } else {
            locks.remove(lockType.name)
        }

        val newLocksJson = serializeLocksJson(locks)

        val toSave = if (existingSettings != null) {
            LockSettings(
                chatId = chatId,
                locksJson = newLocksJson,
                lockWarns = existingSettings.lockWarns,
                createdAt = existingSettings.createdAt,
                updatedAt = Instant.now()
            )
        } else {
            LockSettings.createNew(chatId = chatId, locksJson = newLocksJson)
        }

        lockSettingsRepository.save(toSave)
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
        val existingSettings = lockSettingsRepository.findByChatId(chatId)
        val toSave = if (existingSettings != null) {
            // UPDATE
            LockSettings(
                chatId = chatId,
                locksJson = existingSettings.locksJson,
                lockWarns = enabled,
                createdAt = existingSettings.createdAt,
                updatedAt = Instant.now()
            )
        } else {
            // INSERT
            LockSettings.createNew(chatId = chatId, lockWarns = enabled)
        }
        lockSettingsRepository.save(toSave)
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
    private fun parseLocksJson(json: String): MutableMap<String, LockConfig> {
        if (json.isBlank() || json == "{}") return mutableMapOf()
        return try {
            objectMapper.readValue(json, object : TypeReference<MutableMap<String, LockConfig>>() {})
        } catch (e: Exception) {
            logger.error("Failed to parse locks JSON: ${e.message}", e)
            mutableMapOf()
        }
    }

    private fun serializeLocksJson(locks: Map<String, LockConfig>): String {
        return if (locks.isEmpty()) {
            "{}"
        } else {
            objectMapper.writeValueAsString(locks)
        }
    }
}
