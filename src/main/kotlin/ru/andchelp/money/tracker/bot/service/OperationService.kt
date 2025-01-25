package ru.andchelp.money.tracker.bot.service

import org.springframework.stereotype.Service
import ru.andchelp.money.tracker.bot.infra.CashFlowType
import ru.andchelp.money.tracker.bot.model.Operation
import ru.andchelp.money.tracker.bot.model.OperationRepository
import java.math.RoundingMode

@Service
class OperationService(
    private val operationRepository: OperationRepository,
    private val accountService: AccountService
) {

    fun save(operation: Operation) {
        operationRepository.save(operation)
        val account = accountService.findById(operation.account!!.id!!)
        if (operation.type == CashFlowType.INCOME)
            account.balance = account.balance.add(operation.sum).setScale(2, RoundingMode.HALF_EVEN)
        else
            account.balance = account.balance.minus(operation.sum!!).setScale(2, RoundingMode.HALF_EVEN)
        accountService.save(account)
    }
}