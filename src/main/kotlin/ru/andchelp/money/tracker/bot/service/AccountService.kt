package ru.andchelp.money.tracker.bot.service

import org.springframework.context.annotation.Lazy
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
    private val currencyService: CurrencyService,
    @Lazy private val operationService: OperationService
) {

    fun findByIds(ids: Set<Long>): List<Account> {
        return accountRepository.findAllById(ids)
    }

    fun delete(id: Long) {
        operationService.deleteByAccountId(id)
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
        val account = Account(
            user = user,
            name = accountName,
            currency = currencyService.findByCode(currency),
            balance = BigDecimal.ZERO
        )
        accountRepository.save(account)
    }

    fun findAccounts(userId: Long): List<Account> {
        return accountRepository.findByUserId(userId)
    }

    fun calcTotalBalance(userId: Long): BigDecimal {
        val globalCurrency = userService.findById(userId).globalCurrency
        return accountRepository.findByUserIdAndAllowInTotalBalanceTrue(userId)
            .map {
                val balance = it.balance
                if (it.currency!!.code != globalCurrency!!.code) {
                    currencyService.convert(balance, it.currency!!.code!!, globalCurrency.code!!)
                } else {
                    balance
                }
            }
            .reduce { a, b -> a.add(b).setScale(2, RoundingMode.HALF_EVEN) }
    }

    fun getKeyboard(userId: Long, callbackId: String): MsgKeyboard {
        val keyboard = MsgKeyboard()
        findAccounts(userId).sortedBy { it.id }.map {
            keyboard.row().button("${it.name}, ${it.balance}${it.currency!!.symbol}", callbackId, it.id)
        }
        return keyboard
    }

}