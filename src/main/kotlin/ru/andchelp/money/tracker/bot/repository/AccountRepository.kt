package ru.andchelp.money.tracker.bot.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import ru.andchelp.money.tracker.bot.model.Account

@Repository
interface AccountRepository : JpaRepository<Account, Long> {

    fun findByUserId(userId: Long): List<Account>

    fun findByUserIdAndAllowInTotalBalanceTrue(userId: Long): List<Account>


}

