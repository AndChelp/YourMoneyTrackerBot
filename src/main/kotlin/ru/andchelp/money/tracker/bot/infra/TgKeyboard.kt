package ru.andchelp.money.tracker.bot.infra

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow


class TgKeyboard : ReplyKeyboardMarkup(mutableListOf()) {
    init {
        this.resizeKeyboard = true
    }

    fun row(): TgKeyboard {
        this.keyboard.add(KeyboardRow())
        return this
    }

    fun button(text: Any): TgKeyboard {
        this.keyboard.last().add(KeyboardButton(text.toString()))
        return this
    }

}