package ru.andchelp.money.tracker.bot.service

import org.springframework.stereotype.Service
import ru.andchelp.money.tracker.bot.client.OpenExchangeClient
import ru.andchelp.money.tracker.bot.config.BotProperties
import ru.andchelp.money.tracker.bot.infra.MsgKeyboard
import ru.andchelp.money.tracker.bot.model.Currency
import ru.andchelp.money.tracker.bot.model.CurrencyExchangeRateRepository
import ru.andchelp.money.tracker.bot.model.CurrencyRepository
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class CurrencyService(
    private val currencyRepository: CurrencyRepository,
    private val exchangeRateRepository: CurrencyExchangeRateRepository,
    private val openExchangeClient: OpenExchangeClient,
    private val botProperties: BotProperties
) {

//    fun updateRates() {
//        val map = openExchangeClient.getExchangeRate(botProperties.currencyToken, botProperties.currencyCodes)
//            .rates!!.map {
//                val exchangeRate = exchangeRateRepository.findByCurrencies("USD", it.key)
//                val currencyExchangeRate =
//                    exchangeRate ?: CurrencyExchangeRate(baseCurrencyCode = "USD", rateCurrencyCode = it.key)
//                currencyExchangeRate.rate = it.value
//                currencyExchangeRate
//            }
//        exchangeRateRepository.saveAll(map)
//    }

    fun findByCode(code: String): Currency {
        return currencyRepository.findByCode(code)
    }

    fun getKeyboard(id: String): MsgKeyboard {
        val keyboard = MsgKeyboard()
        currencyRepository.findAll().chunked(3) { chunk ->
            keyboard.row()
            chunk.map {
                keyboard.button("${it.code} ${it.symbol}", id, it.code)
            }
        }
        return keyboard
    }

    fun convert(sum: BigDecimal, baseCurrencyCode: String, rateCurrencyCode: String): BigDecimal {
        val sumBD = sum.setScale(2, RoundingMode.HALF_EVEN)
        val rate = exchangeRateRepository.findByCurrencies(baseCurrencyCode, rateCurrencyCode)?.rate
        if (rate != null)
            return sumBD.multiply(BigDecimal(rate)).setScale(2, RoundingMode.HALF_EVEN)

        val usdToBaseRate = exchangeRateRepository.findByCurrencies("USD", baseCurrencyCode)!!.rate!!
        val usdToRateRate = exchangeRateRepository.findByCurrencies("USD", rateCurrencyCode)!!.rate!!
        return sumBD.divide(BigDecimal(usdToBaseRate), 2, RoundingMode.HALF_EVEN)
            .multiply(BigDecimal(usdToRateRate)).setScale(2, RoundingMode.HALF_EVEN)
    }
}


/*

RUB to EUR via USD


RUB to EUR = 0,0095

USD to RUB = 102.898494
RUB to USD = 1/102.898494

 */
