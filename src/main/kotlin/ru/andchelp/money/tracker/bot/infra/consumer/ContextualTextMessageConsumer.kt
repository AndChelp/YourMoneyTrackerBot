package ru.andchelp.money.tracker.bot.infra.consumer

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update
import ru.andchelp.money.tracker.bot.config.ConsumerOrder
import ru.andchelp.money.tracker.bot.handler.type.ContextualTextMessageHandler
import ru.andchelp.money.tracker.bot.handler.type.TextMessageUpdate
import ru.andchelp.money.tracker.bot.infra.Context
import ru.andchelp.money.tracker.bot.infra.ContextHolder

@Order(ConsumerOrder.TEXT_MESSAGE)
@Component
class ContextualTextMessageConsumer(
    val handlers: Map<String, ContextualTextMessageHandler>
) : GlobalConsumer {
    override fun canConsume(update: Update): Boolean {
        val context: Context? = ContextHolder.current()
        return context?.handlerId != null && update.hasMessage() && update.message.hasText()
    }

    override fun consume(update: Update) {
        val context: Context = ContextHolder.current()!!
        handlers[context.handlerId]?.let {
            it.handle(TextMessageUpdate(update))
            LOG.debug { "Called handler for ${context.handlerId}" }
        } ?: LOG.debug { "Unexpected message ${context.handlerId}" }
    }

    private companion object {
        val LOG = KotlinLogging.logger { }
    }
}