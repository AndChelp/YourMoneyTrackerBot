package ru.andchelp.money.tracker.bot.config

import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow

class MenuConfig {
    companion object {
        val SIMPLE = listOf(KeyboardRow(KeyboardButton("Настройки"), KeyboardButton("Помощь")))
        val FULL =
            listOf(
                KeyboardRow(KeyboardButton("Расход"), KeyboardButton("Доход")),
                KeyboardRow(KeyboardButton("Счета"), KeyboardButton("Категории")),
                KeyboardRow(KeyboardButton("Аналитика"), KeyboardButton("Операции")),
                KeyboardRow(KeyboardButton("Настройки"), KeyboardButton("Помощь")),
            )

    }
}