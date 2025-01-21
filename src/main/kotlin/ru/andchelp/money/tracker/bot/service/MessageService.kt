package ru.andchelp.money.tracker.bot.service

import org.springframework.context.MessageSource
import org.springframework.stereotype.Service
import java.util.Locale

@Service
class MessageService(private val messageSource: MessageSource) {

    fun msgFor(code: String): String {
        return messageSource.getMessage(code, null, Locale.ENGLISH)
    }

}