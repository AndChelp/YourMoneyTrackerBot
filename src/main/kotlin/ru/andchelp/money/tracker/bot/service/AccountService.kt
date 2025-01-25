package ru.andchelp.money.tracker.bot.service

import org.springframework.stereotype.Service
import ru.andchelp.money.tracker.bot.infra.MsgKeyboard
import ru.andchelp.money.tracker.bot.model.Account
import ru.andchelp.money.tracker.bot.repository.AccountRepository
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class AccountService(
    private val accountRepository: AccountRepository,
    private val userService: UserService,
    private val currencyService: CurrencyService
) {

    fun delete(id: Long) {
        accountRepository.deleteById(id)
    }

    fun save(account: Account) {
        accountRepository.save(account)
    }

    fun findById(id: Long): Account {
        return accountRepository.findById(id).orElseThrow()
    }

    fun newAccount(userId: Long, accountName: String, currency: String) {
        val user = userService.findById(userId)
        val account = Account(user = user, name = accountName, currencyCode = currency, balance = BigDecimal.ZERO)
        accountRepository.save(account)
    }

    fun findAccounts(userId: Long): List<Account> {
        return accountRepository.findByUserId(userId)
    }

    fun calcTotalBalance(userId: Long): BigDecimal {
        val globalCurrencyCode = userService.findById(userId).totalBalanceCurrency
        return accountRepository.findByUserIdAndAllowInTotalBalanceTrue(userId)
            .map {
                val balance = it.balance
                if (it.currencyCode != globalCurrencyCode) {
                    currencyService.convert(balance, it.currencyCode!!, globalCurrencyCode!!)
                } else {
                    balance
                }
            }
            .reduce { a, b -> a.add(b).setScale(2, RoundingMode.HALF_EVEN) }
    }

    fun getKeyboard(userId: Long, callbackId: String): MsgKeyboard {
        val keyboard = MsgKeyboard()
        findAccounts(userId).sortedBy { it.id }.map {
            keyboard.row().button("${it.name}, ${it.balance} ${it.currencyCode}", callbackId, it.id)
        }
        return keyboard
    }

}