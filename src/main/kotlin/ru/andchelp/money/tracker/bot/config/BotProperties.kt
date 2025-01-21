package ru.andchelp.money.tracker.bot.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("bot")
data class BotProperties(
    val accessToken: String,
    val currencyToken: String,
    val currencyCodes: String
)
