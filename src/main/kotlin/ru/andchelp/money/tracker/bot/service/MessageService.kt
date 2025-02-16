package ru.andchelp.money.tracker.bot.service

import org.springframework.context.MessageSource
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.message.Message
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.andchelp.money.tracker.bot.infra.ContextHolder
import java.util.Locale

@Service
class MessageService(
    private val messageSource: MessageSource,
    private val telegramClient: TelegramClient
) {

    fun msgFor(code: String): String {
        return messageSource.getMessage(code, null, Locale.ENGLISH)
    }

    fun send(text: String, keyboard: ReplyKeyboard?): Message {
        return telegramClient.execute(
            SendMessage.builder()
                .chatId(ContextHolder.chatId.get())
                .text(msgFor(text))
                .replyMarkup(keyboard)
                .build()
        )
    }

    fun edit(msgId: Int, text: String, keyboard: InlineKeyboardMarkup?) {
        telegramClient.execute(
            EditMessageText.builder()
                .chatId(ContextHolder.chatId.get())
                .messageId(msgId)
                .text(msgFor(text))
                .replyMarkup(keyboard)
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