package ru.andchelp.money.tracker.bot.model

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "accounts")
data class Account(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @ManyToOne
    val user: User? = null,
    val name: String? = null,
    val currencyCode: String? = null,
    val creationDate: LocalDateTime = LocalDateTime.now(),
    val allowInTotalBalance: Boolean = true,

    @OneToOne
    val balance: AccountBalance? = null
)