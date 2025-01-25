package ru.andchelp.money.tracker.bot.handler.type

import org.telegram.telegrambots.meta.api.objects.Update

fun interface ContextualTextMessageHandler {
    fun handle(msg: TextMessageUpdate)
}

data class TextMessageUpdate(
    val chatId: Long,
    val msgId: Int,
    val userId: Long,
    val text: String,
    val update: Update,
) {
    constructor(update: Update) : this(
        update.message.chatId,
        update.message.messageId,
        update.message.from.id,
        update.message.text,
        update
    )
}