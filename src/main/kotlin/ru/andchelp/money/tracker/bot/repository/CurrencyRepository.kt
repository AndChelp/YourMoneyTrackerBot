package ru.andchelp.money.tracker.bot.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import ru.andchelp.money.tracker.bot.model.Currency
import ru.andchelp.money.tracker.bot.model.CurrencyExchangeRate

@Repository
interface CurrencyRepository : JpaRepository<Currency, String>

@Repository
interface CurrencyExchangeRateRepository : JpaRepository<CurrencyExchangeRate, String> {
    @Query("select a from CurrencyExchangeRate a where a.baseCurrencyCode = ?1 and a.rateCurrencyCode = ?2")
    fun findByCurrencies(baseCurrency: String, rateCurrency: String): CurrencyExchangeRate?
}