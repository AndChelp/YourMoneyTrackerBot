package ru.andchelp.money.tracker.bot.handler.type

fun interface GeneralTextMessageHandler {
    fun handle(msg: TextMessageUpdate)
}
