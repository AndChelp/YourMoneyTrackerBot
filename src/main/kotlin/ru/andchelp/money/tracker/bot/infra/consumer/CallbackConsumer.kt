package ru.andchelp.money.tracker.bot.infra.consumer

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Controller
import org.telegram.telegrambots.meta.api.objects.Update
import ru.andchelp.money.tracker.bot.config.ConsumerOrder
import ru.andchelp.money.tracker.bot.handler.type.CallbackHandler
import ru.andchelp.money.tracker.bot.handler.type.CallbackUpdate


@Order(ConsumerOrder.CALLBACK)
@Controller
class CallbackConsumer(
    val handlers: Map<String, CallbackHandler>
) : GlobalConsumer {
    override fun canConsume(update: Update): Boolean {
        return update.hasCallbackQuery()
    }

    override fun consume(update: Update) {
        val clbk = update.callbackQuery.data.substringBefore(":")
        handlers[clbk]?.let {
            it.handle(CallbackUpdate(update))
            LOG.debug { "Called handler for $clbk" }
        } ?: LOG.debug { "Unexpected callback $clbk" }
    }

    private companion object {
        val LOG = KotlinLogging.logger { }
    }
}