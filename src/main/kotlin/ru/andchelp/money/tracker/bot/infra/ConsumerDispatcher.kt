package ru.andchelp.money.tracker.bot.infra

import org.springframework.stereotype.Component
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer
import org.telegram.telegrambots.meta.api.objects.Update
import ru.andchelp.money.tracker.bot.infra.consumer.GlobalConsumer

@Component
class ConsumerDispatcher(val globalConsumers: List<GlobalConsumer>) : LongPollingSingleThreadUpdateConsumer {

    override fun consume(update: Update) {
        globalConsumers
            .first { it.canConsume(update) }
            .consume(update)
    }
}