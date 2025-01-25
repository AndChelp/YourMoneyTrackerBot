package ru.andchelp.money.tracker.bot.infra

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow


class MsgKeyboard : InlineKeyboardMarkup(mutableListOf()) {

    fun row(): MsgKeyboard {
        this.keyboard.add(InlineKeyboardRow())
        return this
    }

    fun button(text: String, id: String, data: Any? = null): MsgKeyboard {
        val button = InlineKeyboardButton(text)
        button.callbackData = data?.let { "$id:$data" } ?: id

        this.keyboard.last().add(button)
        return this
    }

}