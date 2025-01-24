package ru.andchelp.money.tracker.bot.service

import org.springframework.context.MessageSource
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.andchelp.money.tracker.bot.infra.ContextHolder
import java.util.Locale

@Service
class MessageService(
    private val messageSource: MessageSource,
    private val telegramClient: TelegramClient
) {


    private fun msgFor(code: String): String {
        return messageSource.getMessage(code, null, Locale.ENGLISH)
    }


    fun send(text: String) {
        telegramClient.execute(
            SendMessage.builder()
                .chatId(ContextHolder.chatId.get())
                .text(msgFor(text))
                .build()
        )
    }

    fun send(text: String, vararg rows: KeyboardRow) {
        telegramClient.execute(
            SendMessage.builder()
                .chatId(ContextHolder.chatId.get())
                .text(msgFor(text))
                .replyMarkup(ReplyKeyboardMarkup.builder().keyboard(listOf(*rows)).resizeKeyboard(true).build())
                .build()
        )
    }

    fun send(text: String, rows: List<InlineKeyboardRow>) {
        telegramClient.execute(
            SendMessage.builder()
                .chatId(ContextHolder.chatId.get())
                .text(msgFor(text))
                .replyMarkup(InlineKeyboardMarkup(rows))
                .build()
        )
    }

    fun send(text: String, button: InlineKeyboardButton) {
        send(text, InlineKeyboardRow(button))
    }

    fun send(text: String, vararg button: InlineKeyboardRow) {
        send(text, listOf(*button))
    }

    fun edit(msgId: Int, text: String, rows: List<InlineKeyboardRow>) {
        telegramClient.execute(
            EditMessageText.builder()
                .chatId(ContextHolder.chatId.get())
                .messageId(msgId)
                .text(msgFor(text))
                .replyMarkup(InlineKeyboardMarkup(rows))
                .build()
        )
    }

    fun edit(msgId: Int, text: String) {
        telegramClient.execute(
            EditMessageText.builder()
                .chatId(ContextHolder.chatId.get())
                .messageId(msgId)
                .text(msgFor(text))
                .build()
        )
    }

    fun delete(msgId: Int) {
        telegramClient.execute(
            DeleteMessage(ContextHolder.chatId.get().toString(), msgId)
        )
    }
}