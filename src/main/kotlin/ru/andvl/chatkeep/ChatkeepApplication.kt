package ru.andvl.chatkeep

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling
import ru.andvl.chatkeep.api.config.MediaUploadConfig

@SpringBootApplication
@EnableConfigurationProperties(MediaUploadConfig::class)
@EnableScheduling
class ChatkeepApplication

fun main(args: Array<String>) {
    runApplication<ChatkeepApplication>(*args)
}
