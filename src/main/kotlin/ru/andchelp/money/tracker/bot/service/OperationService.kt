package ru.andchelp.money.tracker.bot.service

import jakarta.persistence.criteria.Predicate
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import ru.andchelp.money.tracker.bot.infra.CashFlowType
import ru.andchelp.money.tracker.bot.infra.MsgKeyboard
import ru.andchelp.money.tracker.bot.model.Account
import ru.andchelp.money.tracker.bot.model.Category
import ru.andchelp.money.tracker.bot.model.Operation
import ru.andchelp.money.tracker.bot.model.OperationRepository
import ru.andchelp.money.tracker.bot.model.User
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

@Service
class OperationService(
    private val operationRepository: OperationRepository,
    private val accountService: AccountService,
) {

    val defaultSums: List<BigDecimal> = listOf(
        BigDecimal(100).setScale(2),
        BigDecimal(500).setScale(2),
        BigDecimal(1000).setScale(2),
        BigDecimal(1500).setScale(2),
        BigDecimal(2000).setScale(2),
        BigDecimal(2500).setScale(2),
        BigDecimal(3000).setScale(2),
        BigDecimal(3500).setScale(2)
    )

    fun getFrequentlyUsedSumKeyboard(userId: Long, type: CashFlowType, callbackId: String): MsgKeyboard {
        val keyboard = MsgKeyboard()
        val fSums = operationRepository.findFrequentlyUsedSum(userId, type)
        if (fSums.size < 8) {
            val result = defaultSums.filter { it !in fSums }

            fSums.addAll(result.subList(0, 8 - fSums.size))
        }

        fSums.sorted()
            .chunked(4)
            .forEach { sums ->
                keyboard.row()
                sums.forEach { keyboard.button(it, callbackId, it) }
            }
        return keyboard
    }


    fun deleteByAccountId(accountId: Long) {
        operationRepository.deleteByAccountId(accountId)
    }

    fun save(operation: Operation) {
        operationRepository.save(operation)
        val account = accountService.findById(operation.account!!.id!!)
        if (operation.type == CashFlowType.INCOME)
            account.balance = account.balance.add(operation.sum).setScale(2, RoundingMode.HALF_EVEN)
        else
            account.balance = account.balance.minus(operation.sum!!).setScale(2, RoundingMode.HALF_EVEN)
        accountService.save(account)
    }

    fun find(operationFilter: OperationFilter, pageable: Pageable): Page<Operation> {
        return operationRepository.findAll(operationSpecification(operationFilter), pageable)
    }

    fun findById(id: Long): Operation {
        return operationRepository.findById(id).orElseThrow()
    }

    fun operationSpecification(filter: OperationFilter): Specification<Operation> {
        return Specification { root, query, cb ->
            val predicates = mutableListOf<Predicate>()

            predicates.add(cb.equal(root.get<User>("user").get<Long>("id"), filter.userId))

            // Фильтр по accountId
            filter.accountIds.takeIf { it.isNotEmpty() }?.let {
                predicates.add(root.get<Account>("account").get<Long>("id").`in`(it))
            }

            // Фильтр по типам операций
            filter.types.takeIf { it.isNotEmpty() }?.let {
                predicates.add(root.get<CashFlowType>("type").`in`(it))
            }

            // Фильтр по categoryId
            filter.categoryIds.takeIf { it.isNotEmpty() }?.let {
                predicates.add(root.get<Category>("category").get<Long>("id").`in`(it))
            }

            // Фильтр по сумме (от)
            filter.sumFrom?.let {
                predicates.add(cb.greaterThanOrEqualTo(root.get("sum"), it))
            }

            // Фильтр по сумме (до)
            filter.sumTill?.let {
                predicates.add(cb.lessThanOrEqualTo(root.get("sum"), it))
            }

            // Фильтр по дате (от)
            filter.dateFrom.let {
                predicates.add(cb.greaterThanOrEqualTo(root.get("date"), it))
            }

            // Фильтр по дате (до)
            filter.dateTill.let {
                predicates.add(cb.lessThanOrEqualTo(root.get("date"), it.plusDays(1).toLocalDate().atStartOfDay()))
            }

            query.orderBy(cb.desc(root.get<LocalDateTime>("date")), cb.desc(root.get<Long>("id")))
            cb.and(*predicates.toTypedArray())
        }
    }
}

data class OperationFilter(
    var userId: Long,
    var accountIds: MutableSet<Long> = mutableSetOf(),
    var categoryIds: MutableSet<Long> = mutableSetOf(),
    var sumFrom: BigDecimal? = null,
    var sumTill: BigDecimal? = null,
    var types: MutableSet<CashFlowType> = mutableSetOf(CashFlowType.INCOME, CashFlowType.OUTCOME),
    var dateFrom: LocalDateTime = LocalDateTime.now().minusDays(7),
    var dateTill: LocalDateTime = LocalDateTime.now(),
)