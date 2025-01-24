package ru.andchelp.money.tracker.bot.service

import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow
import ru.andchelp.money.tracker.bot.client.OpenExchangeClient
import ru.andchelp.money.tracker.bot.config.BotProperties
import ru.andchelp.money.tracker.bot.data
import ru.andchelp.money.tracker.bot.id
import ru.andchelp.money.tracker.bot.model.CurrencyExchangeRate
import ru.andchelp.money.tracker.bot.repository.CurrencyExchangeRateRepository
import ru.andchelp.money.tracker.bot.repository.CurrencyRepository
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class CurrencyService(
    private val currencyRepository: CurrencyRepository,
    private val exchangeRateRepository: CurrencyExchangeRateRepository,
    private val openExchangeClient: OpenExchangeClient,
    private val botProperties: BotProperties
) {

    fun updateRates() {
        val map = openExchangeClient.getExchangeRate(botProperties.currencyToken, botProperties.currencyCodes)
            .rates!!.map {
                val exchangeRate = exchangeRateRepository.findByCurrencies("USD", it.key)
                val currencyExchangeRate =
                    exchangeRate ?: CurrencyExchangeRate(baseCurrencyCode = "USD", rateCurrencyCode = it.key)
                currencyExchangeRate.rate = it.value
                currencyExchangeRate
            }
        exchangeRateRepository.saveAll(map)
    }

    fun getKeyboard(id: String): MutableList<InlineKeyboardRow> =
        currencyRepository.findAll().chunked(3) { chunk ->
            chunk.map {
                InlineKeyboardButton("${it.code} ${it.symbol}").id(id).data(it.code!!)
            }.let { InlineKeyboardRow(it) }
        }.toMutableList()


    fun convert(sum: Double, baseCurrencyCode: String, rateCurrencyCode: String): Double {
        val sumBD = BigDecimal.valueOf(sum).setScale(2, RoundingMode.HALF_EVEN)
        val rate =
            exchangeRateRepository.findByCurrencies(baseCurrencyCode, rateCurrencyCode)?.rate
        if (rate == null) {
            val usdToBaseRate = exchangeRateRepository.findByCurrencies("USD", baseCurrencyCode)!!.rate!!
            val usdToRateRate = exchangeRateRepository.findByCurrencies("USD", rateCurrencyCode)!!.rate!!

            val baseToUsd = sumBD.multiply(BigDecimal(1.0 / usdToBaseRate)).setScale(2, RoundingMode.HALF_EVEN)
            return baseToUsd.multiply(BigDecimal(usdToRateRate)).setScale(2, RoundingMode.HALF_EVEN).toDouble()
        }
        val rateBD = BigDecimal.valueOf(rate).setScale(2, RoundingMode.HALF_EVEN)
        return sumBD.multiply(rateBD).setScale(2, RoundingMode.HALF_EVEN).toDouble()
    }
}


/*

RUB to EUR via USD


RUB to EUR = 0,0095

USD to RUB = 102.898494
RUB to USD = 1/102.898494

 */
