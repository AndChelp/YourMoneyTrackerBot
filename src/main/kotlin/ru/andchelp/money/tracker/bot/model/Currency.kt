package ru.andchelp.money.tracker.bot.model

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "currencies")
data class Currency(
    @Id
    val code: String? = null,
    val name: String? = null,
    val symbol: String? = null,
    @OneToMany(mappedBy = "baseCurrencyCode")
    val exchangeRates: List<CurrencyExchangeRate>? = null,
)

@Entity
@Table(name = "currency_exchange_rate")
data class CurrencyExchangeRate(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    val baseCurrencyCode: String? = null,
    val rateCurrencyCode: String? = null,
    var rate: Double? = null
)