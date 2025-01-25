package ru.andchelp.money.tracker.bot

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
