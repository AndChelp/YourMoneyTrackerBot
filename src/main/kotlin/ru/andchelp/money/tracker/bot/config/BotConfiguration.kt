package ru.andchelp.money.tracker.bot.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.andchelp.money.tracker.bot.infra.ConsumerDispatcher


@Configuration
class BotConfiguration {

//    @Bean
//    fun messageSource(): MessageSource {
//        val messageSource = ResourceBundleMessageSource()
//        messageSource.setBasenames("classpath:messages")
//        messageSource.setDefaultEncoding("UTF-8")
//        messageSource.setFallbackToSystemLocale(true)
//        messageSource.setDefaultLocale(Locale.forLanguageTag("ru"))
//        return messageSource
//    }

    @Bean
    fun telegramClient(botProperties: BotProperties): TelegramClient {
        return OkHttpTelegramClient(botProperties.accessToken)
    }

    @Bean
    fun longPollingBot(botProperties: BotProperties, consumerDispatcher: ConsumerDispatcher): SpringLongPollingBot {
        return object : SpringLongPollingBot {
            override fun getBotToken(): String {
                return botProperties.accessToken
            }

            override fun getUpdatesConsumer(): LongPollingUpdateConsumer {
                return consumerDispatcher
            }
        }
    }
}