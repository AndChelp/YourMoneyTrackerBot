package ru.andchelp.money.tracker.bot.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.andchelp.money.tracker.bot.infra.MsgKeyboard
import ru.andchelp.money.tracker.bot.model.Account
import ru.andchelp.money.tracker.bot.model.AccountBalance
import ru.andchelp.money.tracker.bot.repository.AccountBalanceRepository
import ru.andchelp.money.tracker.bot.repository.AccountRepository

@Service
class AccountService(
    private val accountRepository: AccountRepository,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val userService: UserService,
    private val currencyService: CurrencyService
) {

    @Transactional
    fun delete(id: Long) {
        accountRepository.deleteById(id)
        accountBalanceRepository.deleteById(id)
    }

    fun save(account: Account) {
        accountRepository.save(account)
    }

    fun findById(id: Long): Account {
        return accountRepository.findById(id).orElseThrow()
    }

    @Transactional
    fun newAccount(userId: Long, accountName: String, currency: String) {
        val balance = accountBalanceRepository.save(AccountBalance())
        val user = userService.findById(userId)
        val account = Account(user = user, name = accountName, currencyCode = currency, balance = balance)
        accountRepository.save(account)
    }

    fun findAccounts(userId: Long): List<Account> {
        return accountRepository.findByUserId(userId)
    }

    fun calcTotalBalance(userId: Long): Double {
        val globalCurrencyCode = userService.findById(userId).totalBalanceCurrency
        return findAccounts(userId)
            .map {
                val balance = it.balance!!.balance
                if (it.currencyCode != globalCurrencyCode) {
                    currencyService.convert(balance, it.currencyCode!!, globalCurrencyCode!!)
                } else {
                    balance
                }
            }
            .reduce { a, b -> a + b }
    }

    fun getKeyboard(userId: Long, callbackId: String): MsgKeyboard {
        val keyboard = MsgKeyboard()
        findAccounts(userId).sortedBy { it.id }.map {
            keyboard.row().button("${it.name} - ${it.balance!!.balance} ${it.currencyCode}", callbackId, it.id)
        }
        return keyboard
    }

}