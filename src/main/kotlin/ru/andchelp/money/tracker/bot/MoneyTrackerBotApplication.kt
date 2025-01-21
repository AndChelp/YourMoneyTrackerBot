package ru.andchelp.money.tracker.bot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
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

fun InlineKeyboardMarkup.addBackButton(clbkData: String): InlineKeyboardMarkup {
    this.keyboard.add(
        InlineKeyboardRow(
            InlineKeyboardButton.builder()
                .text("Назад")
                .callbackData(clbkData)
                .build()
        )
    )
    return this
}