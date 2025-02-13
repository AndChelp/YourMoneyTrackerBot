package ru.andchelp.money.tracker.bot.handler.type

import org.telegram.telegrambots.meta.api.objects.Update

fun interface CommandHandler {
    fun handle(cmd: CommandUpdate)
}

data class CommandUpdate(
    val msgId: Int,
    val userId: Long,
    val update: Update
) {
    constructor(update: Update) : this(
        update.message.messageId,
        update.message.from.id,
        update
    )
}