package ru.andchelp.money.tracker.bot.service

import org.springframework.stereotype.Service
import ru.andchelp.money.tracker.bot.infra.CashFlowType
import ru.andchelp.money.tracker.bot.infra.MsgKeyboard
import ru.andchelp.money.tracker.bot.model.Category
import ru.andchelp.money.tracker.bot.model.CategoryRepository
import ru.andchelp.money.tracker.bot.model.User

@Service
class CategoryService(
    private val categoryRepository: CategoryRepository,
    private val userService: UserService,
) {

    fun addDefaultCategories(user: User) {
        val defaultCategories = listOf(
            Category(type = CashFlowType.INCOME, name = "Зарплата"),
            Category(type = CashFlowType.INCOME, name = "Бизнес"),
            Category(type = CashFlowType.INCOME, name = "Услуги"),
            Category(type = CashFlowType.INCOME, name = "Вклады"),
            Category(type = CashFlowType.INCOME, name = "Крипта"),
            Category(type = CashFlowType.INCOME, name = "Фриланс"),

            Category(type = CashFlowType.OUTCOME, name = "Покупки"),
            Category(type = CashFlowType.OUTCOME, name = "Еда"),
            Category(type = CashFlowType.OUTCOME, name = "Хобби"),
            Category(type = CashFlowType.OUTCOME, name = "Путешествия"),
            Category(type = CashFlowType.OUTCOME, name = "Здоровье"),
        )
        defaultCategories.forEach { it.user = user }
        categoryRepository.saveAll(defaultCategories)
    }

    fun getRootCategoriesKeyboard(userId: Long, type: CashFlowType, clbkId: String): MsgKeyboard {
        val categories = categoryRepository.findByUserIdAndTypeAndParenCategoryIsNull(userId, type)
        return msgKeyboard(categories, clbkId)
    }

    fun delete(id: Long) {
        categoryRepository.deleteById(id)
    }

    fun findById(id: Long): Category {
        return categoryRepository.findById(id).orElseThrow()
    }

    fun getSubcategoriesKeyboard(parentId: Long, clbkId: String): MsgKeyboard {
        val categories = findByParentId(parentId)
        return msgKeyboard(categories, clbkId)
    }

    fun findByParentId(parentId: Long) = categoryRepository.findByParenCategoryId(parentId)

    fun save(userId: Long, name: String, type: CashFlowType, parentId: Long?) {

        val user = userService.findById(userId)

        val parent = parentId?.let {
            categoryRepository.findById(parentId).let {
                if (it.isPresent) it.get() else null
            }
        }

        categoryRepository.save(Category(user = user, name = name, type = type, parenCategory = parent))

    }

    private fun msgKeyboard(categories: List<Category>, clbkId: String): MsgKeyboard {
        if (categories.isEmpty())
            return MsgKeyboard()
        val msgKeyboard = MsgKeyboard()
        categories.chunked(2).forEach {
            val withNewRow = msgKeyboard.row()
            it.forEach { category -> withNewRow.button(category.name!!, clbkId, "${category.id!!}") }
        }
        return msgKeyboard
    }


}