package ru.andchelp.money.tracker.bot.model

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Entity
@Table(name = "currencies")
data class Currency(
    @Id
    val code: String? = null,
    val name: String? = null,
    val symbol: String? = null,
)

@Entity
@Table(name = "currency_exchange_rate")
data class CurrencyExchangeRate(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @ManyToOne
    val baseCurrency: Currency? = null,
    @ManyToOne
    val rateCurrency: Currency? = null,
    var rate: Double? = null
)


@Repository
interface CurrencyRepository : JpaRepository<Currency, String> {
    fun findByCode(code: String): Currency
}

@Repository
interface CurrencyExchangeRateRepository : JpaRepository<CurrencyExchangeRate, String> {
    @Query("select a from CurrencyExchangeRate a where a.baseCurrency.code = ?1 and a.rateCurrency.code = ?2")
    fun findByCurrencies(baseCurrency: String, rateCurrency: String): CurrencyExchangeRate?
}