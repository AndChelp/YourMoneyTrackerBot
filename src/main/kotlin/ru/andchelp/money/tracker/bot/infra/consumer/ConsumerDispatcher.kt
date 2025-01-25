package ru.andchelp.money.tracker.bot.infra.consumer

import org.springframework.stereotype.Component
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer
import org.telegram.telegrambots.meta.api.objects.Update
import ru.andchelp.money.tracker.bot.infra.ContextHolder

@Component
class ConsumerDispatcher(val globalConsumers: List<GlobalConsumer>) : LongPollingSingleThreadUpdateConsumer {

    override fun consume(update: Update) {
        ContextHolder.chatId.set(update.message?.from?.id ?: update.callbackQuery.from.id)
        globalConsumers
            .first { it.canConsume(update) }
            .consume(update)
    }
}