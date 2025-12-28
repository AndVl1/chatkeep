package ru.andvl.chatkeep

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ChatkeepApplication

fun main(args: Array<String>) {
    runApplication<ChatkeepApplication>(*args)
}
