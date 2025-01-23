package ru.andchelp.money.tracker.bot.infra.consumer

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update
import ru.andchelp.money.tracker.bot.config.ConsumerOrder
import ru.andchelp.money.tracker.bot.handler.type.TextMessageHandler
import ru.andchelp.money.tracker.bot.handler.type.TextMessageUpdate

@Order(ConsumerOrder.TEXT_MESSAGE)
@Component
class TextMessageConsumer(
    val handlers: List<TextMessageHandler>
) : GlobalConsumer {
    override fun canConsume(update: Update): Boolean {
        return update.hasMessage() && update.message.hasText()
    }

    override fun consume(update: Update) {
        handlers.forEach { it.handle(TextMessageUpdate(update)) }
        LOG.info { "TextMessageConsumer called" }
    }

    private companion object {
        val LOG = KotlinLogging.logger { }
    }
}