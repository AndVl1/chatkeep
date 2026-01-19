package ru.andvl.chatkeep

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import ru.andvl.chatkeep.api.config.MediaUploadConfig

@SpringBootApplication
@EnableConfigurationProperties(MediaUploadConfig::class)
class ChatkeepApplication

fun main(args: Array<String>) {
    runApplication<ChatkeepApplication>(*args)
}
