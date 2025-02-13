package ru.andchelp.money.tracker.bot.handler.type

import org.telegram.telegrambots.meta.api.objects.Update

fun interface CallbackHandler {
    fun handle(clbk: CallbackUpdate)
}

data class CallbackUpdate(
    val msgId: Int,
    val userId: Long,
    val data: String,
    val update: Update
) {
    constructor(update: Update) : this(
        update.callbackQuery.message.messageId,
        update.callbackQuery.from.id,
        update.callbackQuery.data.substringAfter(':', ""),
        update
    )
}


