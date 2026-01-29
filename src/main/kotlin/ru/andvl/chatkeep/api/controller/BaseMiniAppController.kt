package ru.andvl.chatkeep.api.controller

import jakarta.servlet.http.HttpServletRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import ru.andvl.chatkeep.api.auth.TelegramAuthFilter
import ru.andvl.chatkeep.api.auth.TelegramAuthService
import ru.andvl.chatkeep.domain.service.moderation.AdminCacheService

abstract class BaseMiniAppController(
    protected val adminCacheService: AdminCacheService
) {
    protected fun getUserFromRequest(request: HttpServletRequest): TelegramAuthService.TelegramUser {
        return request.getAttribute(TelegramAuthFilter.USER_ATTR) as? TelegramAuthService.TelegramUser
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated")
    }

    protected fun requireAdmin(
        request: HttpServletRequest,
        chatId: Long,
        forceRefresh: Boolean = false
    ): TelegramAuthService.TelegramUser {
        val user = getUserFromRequest(request)
        val isAdmin = runBlocking(Dispatchers.IO) {
            adminCacheService.isAdmin(user.id, chatId, forceRefresh)
        }
        if (!isAdmin) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "You are not an admin in this chat")
        }
        return user
    }
}
