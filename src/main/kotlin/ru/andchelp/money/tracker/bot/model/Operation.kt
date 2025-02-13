package ru.andchelp.money.tracker.bot.model

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import ru.andchelp.money.tracker.bot.infra.CashFlowType
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "operations")
data class Operation(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @ManyToOne
    var account: Account? = null,
    @ManyToOne
    var category: Category? = null,
    @ManyToOne
    var user: User? = null,
    val type: CashFlowType? = null,
    var sum: BigDecimal? = null,
    var date: LocalDateTime = LocalDateTime.now(),
    var repeatFrequency: Int? = null
)

@Repository
interface OperationRepository : JpaRepository<Operation, Long>, JpaSpecificationExecutor<Operation> {
    fun deleteByAccountId(accountId: Long)

    @Query("select o.sum, count(*) as freq from Operation o where o.user.id=?1 and o.type = ?2 group by o.sum order by o.sum desc limit 8")
    fun findFrequentlyUsedSum(userId: Long, type: CashFlowType): MutableList<BigDecimal>
}