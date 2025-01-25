package ru.andchelp.money.tracker.bot.model

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import ru.andchelp.money.tracker.bot.infra.CashFlowType

@Entity
@Table(name = "categories")
data class Category(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne
    var parenCategory: Category? = null,

    @ManyToOne
    var user: User? = null,

    var name: String? = null,
    var type: CashFlowType? = null,
)

@Repository
interface CategoryRepository : JpaRepository<Category, Long> {

    fun findByUserIdAndTypeAndParenCategoryIsNull(userId: Long, type: CashFlowType): List<Category>

    fun findByParenCategoryId(id: Long): List<Category>


}