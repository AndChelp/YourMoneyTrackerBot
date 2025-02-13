package ru.andchelp.money.tracker.bot.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.andchelp.money.tracker.bot.infra.MsgKeyboard
import ru.andchelp.money.tracker.bot.model.ShoppingListItem
import ru.andchelp.money.tracker.bot.model.ShoppingListItemRepository

@Service
class ShoppingListService(
    private val shoppingListItemRepository: ShoppingListItemRepository
) {

    fun save(item: ShoppingListItem) {
        shoppingListItemRepository.save(item)
    }

    @Transactional
    fun delete(id: Long, userId: Long) {
        shoppingListItemRepository.deleteByIdAndUserId(id, userId)
    }

    @Transactional
    fun deleteBought(userId: Long) {
        shoppingListItemRepository.deleteByUserIdAndBoughtTrue(userId)
    }

    fun findByIdAndUserId(id: Long, userId: Long): ShoppingListItem {
        return shoppingListItemRepository.findByIdAndUserId(id, userId)
    }

    fun findByUserId(userId: Long): List<ShoppingListItem> {
        return shoppingListItemRepository.findByUserIdOrderByIdDesc(userId)
    }

    fun findBoughtByUserId(userId: Long): List<ShoppingListItem> {
        return shoppingListItemRepository.findByUserIdAndBoughtTrue(userId)
    }

    fun getItemsKeyboard(userId: Long, clbkId: String): MsgKeyboard {
        val keyboard = MsgKeyboard()
        findByUserId(userId).forEach {
            keyboard.row().button("(${if (it.bought) "✓" else "✗"})    ${it.name}, ${it.sum}", clbkId, it.id)
        }
        return keyboard
    }

}