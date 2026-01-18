package ru.andvl.chatkeep.api.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "media.upload")
data class MediaUploadConfig(
    var maxFileSizeMb: Long = 10,
    var allowedMediaTypes: Set<String> = setOf("PHOTO", "VIDEO", "DOCUMENT", "ANIMATION"),
    var allowedMimeTypes: Set<String> = setOf(
        "image/jpeg",
        "image/png",
        "image/gif",
        "image/webp",
        "video/mp4",
        "video/quicktime",
        "video/x-msvideo",
        "video/mpeg",
        "application/pdf",
        "application/zip",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    )
)
