package ru.andchelp.money.tracker.bot.infra.consumer

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.EntityType
import org.telegram.telegrambots.meta.api.objects.Update
import ru.andchelp.money.tracker.bot.config.ConsumerOrder
import ru.andchelp.money.tracker.bot.handler.type.CommandHandler

@Order(ConsumerOrder.COMMAND)
@Component
class CommandConsumer(
    val handlers: Map<String, CommandHandler>
) : GlobalConsumer {
    override fun canConsume(update: Update): Boolean {
        return update.hasMessage() && update.message.isCommand()
    }

    override fun consume(update: Update) {
        val cmd = update.message.entities.find { it.type == EntityType.BOTCOMMAND }?.text
            ?: throw RuntimeException("CommandConsumer called without a command")

        handlers[cmd]?.let {
            it.handle(update)
            LOG.debug { "Called handler for $cmd" }
        } ?: LOG.debug { "Unexpected command $cmd" }
    }

    private companion object {
        val LOG = KotlinLogging.logger { }
    }
}