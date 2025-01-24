package ru.andchelp.money.tracker.bot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow
import ru.andchelp.money.tracker.bot.config.BotProperties

@EnableConfigurationProperties(BotProperties::class)
@SpringBootApplication
@EnableFeignClients
class MoneyTrackerBotApplication

fun main(args: Array<String>) {
    runApplication<MoneyTrackerBotApplication>(*args)
}

fun InlineKeyboardButton.id(id: String): InlineKeyboardButton {
    this.callbackData = id
    return this
}

fun InlineKeyboardButton.data(data: String): InlineKeyboardButton {
    this.callbackData += ":$data"
    return this
}


fun MutableList<InlineKeyboardRow>.with(element: InlineKeyboardRow): MutableList<InlineKeyboardRow> {
    this.add(element)
    return this
}

fun MutableList<InlineKeyboardRow>.withRow(vararg element: InlineKeyboardButton): MutableList<InlineKeyboardRow> {
    this.add(InlineKeyboardRow(listOf(*element)))
    return this
}