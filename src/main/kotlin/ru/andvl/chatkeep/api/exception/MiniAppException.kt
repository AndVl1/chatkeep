package ru.andvl.chatkeep.api.exception

open class MiniAppException(
    val code: String,
    override val message: String,
    val details: Map<String, Any>? = null
) : RuntimeException(message)

class ResourceNotFoundException(
    resource: String,
    id: Any
) : MiniAppException(
    code = "RESOURCE_NOT_FOUND",
    message = "$resource not found: $id"
)

class AccessDeniedException(
    message: String = "Access denied"
) : MiniAppException(
    code = "ACCESS_DENIED",
    message = message
)

class ValidationException(
    message: String,
    details: Map<String, Any>? = null
) : MiniAppException(
    code = "VALIDATION_ERROR",
    message = message,
    details = details
)

class UnauthorizedException(
    message: String = "Unauthorized"
) : MiniAppException(
    code = "UNAUTHORIZED",
    message = message
)
