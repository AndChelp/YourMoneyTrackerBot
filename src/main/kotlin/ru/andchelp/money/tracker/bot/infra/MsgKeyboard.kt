package ru.andchelp.money.tracker.bot.infra

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow


class MsgKeyboard : InlineKeyboardMarkup(mutableListOf()) {

    fun row(): MsgKeyboard {
        this.keyboard.add(InlineKeyboardRow())
        return this
    }

    fun button(text: String, id: String, data: String? = null): MsgKeyboard {
        this.keyboard.last().add(InlineKeyboardButton(text).id(id).data(data))
        return this
    }

    private fun InlineKeyboardButton.id(id: String): InlineKeyboardButton {
        this.callbackData = id
        return this
    }

    private fun InlineKeyboardButton.data(data: String? = null): InlineKeyboardButton {
        if (data != null) {
            this.callbackData += ":$data"
        }
        return this
    }
}