package ru.andvl.chatkeep.infrastructure.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import ru.andvl.chatkeep.domain.model.UserPreferences

@Repository
interface UserPreferencesRepository : CrudRepository<UserPreferences, Long>
