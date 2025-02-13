package ru.andchelp.money.tracker.bot.model

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.math.BigDecimal

@Entity
@Table(name = "shopping_list_items")
data class ShoppingListItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @ManyToOne
    var user: User? = null,

    var name: String? = null,
    var sum: BigDecimal? = null,
    var bought: Boolean = false
)

@Repository
interface ShoppingListItemRepository : JpaRepository<ShoppingListItem, Long> {
    fun deleteByIdAndUserId(id: Long, userId: Long)
    fun deleteByUserIdAndBoughtTrue(userId: Long)

    fun findByIdAndUserId(id: Long, userId: Long): ShoppingListItem

    fun findByUserIdOrderByIdDesc(userId: Long): List<ShoppingListItem>
    fun findByUserIdAndBoughtTrue(userId: Long): List<ShoppingListItem>
}