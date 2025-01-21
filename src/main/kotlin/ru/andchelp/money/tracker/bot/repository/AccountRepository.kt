package ru.andchelp.money.tracker.bot.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import ru.andchelp.money.tracker.bot.model.Account
import ru.andchelp.money.tracker.bot.model.AccountBalance

@Repository
interface AccountRepository : JpaRepository<Account, Long> {

    @Query("select a from Account a where a.user.id = ?1")
    fun findByUserId(userId: Long): List<Account>
}

@Repository
interface AccountBalanceRepository : JpaRepository<AccountBalance, Long>
