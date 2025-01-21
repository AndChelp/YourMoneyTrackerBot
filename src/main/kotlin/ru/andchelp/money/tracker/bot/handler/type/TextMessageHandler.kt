package ru.andchelp.money.tracker.bot.handler.type

import org.telegram.telegrambots.meta.api.objects.Update

fun interface TextMessageHandler {
    fun handle(update: Update)
}