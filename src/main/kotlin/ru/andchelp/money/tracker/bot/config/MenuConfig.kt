package ru.andchelp.money.tracker.bot.config

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow

class MenuConfig {
    companion object {
        val SIMPLE: ReplyKeyboardMarkup = ReplyKeyboardMarkup.builder()
            .keyboardRow(KeyboardRow(KeyboardButton("Настройки"), KeyboardButton("Помощь")))
            .resizeKeyboard(true)
            .build()
        val FULL: ReplyKeyboardMarkup = ReplyKeyboardMarkup.builder()
            .keyboard(
                listOf(
                    KeyboardRow(KeyboardButton("Расход"), KeyboardButton("Доход")),
                    KeyboardRow(KeyboardButton("Счета"), KeyboardButton("Категории")),
                    KeyboardRow(KeyboardButton("Аналитика"), KeyboardButton("Операции")),
                    KeyboardRow(KeyboardButton("Настройки"), KeyboardButton("Помощь")),
                )
            ).resizeKeyboard(true)
            .build()
    }
}