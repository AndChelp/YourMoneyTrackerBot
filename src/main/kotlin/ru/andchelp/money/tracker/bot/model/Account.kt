package ru.andchelp.money.tracker.bot.model

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "accounts")
data class Account(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @ManyToOne
    val user: User? = null,
    var name: String? = null,
    var currencyCode: String? = null,
    val creationDate: LocalDateTime = LocalDateTime.now(),
    var allowInTotalBalance: Boolean = true,

    var balance: BigDecimal = BigDecimal.ZERO
)