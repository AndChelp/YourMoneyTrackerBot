package ru.andchelp.money.tracker.bot.infra.consumer

import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer
import org.telegram.telegrambots.meta.api.objects.Update

interface GlobalConsumer: LongPollingSingleThreadUpdateConsumer {
    fun canConsume(update: Update): Boolean
}