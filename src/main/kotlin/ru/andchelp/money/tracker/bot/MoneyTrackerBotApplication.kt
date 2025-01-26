package ru.andchelp.money.tracker.bot

import org.apache.commons.lang3.StringUtils
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients
import ru.andchelp.money.tracker.bot.config.BotProperties

@EnableConfigurationProperties(BotProperties::class)
@SpringBootApplication
@EnableFeignClients
class MoneyTrackerBotApplication

fun main(args: Array<String>) {
    runApplication<MoneyTrackerBotApplication>(*args)
}

fun <T> MutableSet<T>.toggleItem(obj: T) {
    if (this.contains(obj))
        this.remove(obj)
    else
        this.add(obj)
}

fun String.abbreviate(maxLength: Int = 6): String {
    return this.trim().split(" ")
        .joinToString(" ") { StringUtils.abbreviate(it, ".", maxLength) }
}
