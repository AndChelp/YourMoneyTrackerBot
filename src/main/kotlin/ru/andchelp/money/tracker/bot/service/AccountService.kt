package ru.andchelp.money.tracker.bot.service

import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow
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
                    val convert = currencyService.convert(balance, it.currencyCode!!, globalCurrencyCode!!)
                    convert
                } else {
                    balance
                }
            }
            .reduce { a, b -> a + b }
    }

    fun getKeyboard(userId: Long): MutableList<InlineKeyboardRow> =
        findAccounts(userId).map {
            InlineKeyboardRow(
                InlineKeyboardButton.builder()
                    .text("${it.name} - ${it.balance!!.balance} ${it.currencyCode}")
                    .callbackData("account_clbk:${it.id}")
                    .build()
            )
        }.toMutableList()


}