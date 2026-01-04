package ru.andvl.chatkeep.domain.service.locks

import org.springframework.stereotype.Component
import ru.andvl.chatkeep.domain.model.locks.LockCategory
import ru.andvl.chatkeep.domain.model.locks.LockType

@Component
class LockDetectorRegistry(
    detectors: List<LockDetector>
) {
    private val detectorMap: Map<LockType, LockDetector> =
        detectors.associateBy { it.lockType }

    fun getDetector(lockType: LockType): LockDetector? = detectorMap[lockType]

    fun getAllDetectors(): Collection<LockDetector> = detectorMap.values

    fun getDetectorsByCategory(category: LockCategory): List<LockDetector> =
        detectorMap.values.filter { it.lockType.category == category }
}
