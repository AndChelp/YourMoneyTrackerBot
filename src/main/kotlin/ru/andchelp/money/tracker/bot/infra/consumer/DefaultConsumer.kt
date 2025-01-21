package ru.andchelp.money.tracker.bot.infra.consumer

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update
import ru.andchelp.money.tracker.bot.config.ConsumerOrder

@Order(ConsumerOrder.DEFAULT)
@Component
class DefaultConsumer : GlobalConsumer {
    override fun canConsume(update: Update): Boolean {
        return true
    }

    override fun consume(update: Update) {
        LOG.info { "DefaultConsumer called" }
    }

    private companion object {
        val LOG = KotlinLogging.logger { }
    }
}