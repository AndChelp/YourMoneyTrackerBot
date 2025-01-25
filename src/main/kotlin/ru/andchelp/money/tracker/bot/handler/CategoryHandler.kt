package ru.andchelp.money.tracker.bot.handler

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.andchelp.money.tracker.bot.handler.type.CallbackHandler
import ru.andchelp.money.tracker.bot.handler.type.ContextualTextMessageHandler
import ru.andchelp.money.tracker.bot.handler.type.GeneralTextMessageHandler
import ru.andchelp.money.tracker.bot.infra.CashFlowType
import ru.andchelp.money.tracker.bot.infra.ContextHolder
import ru.andchelp.money.tracker.bot.infra.MsgKeyboard
import ru.andchelp.money.tracker.bot.infra.NewCategoryContext
import ru.andchelp.money.tracker.bot.service.CategoryService
import ru.andchelp.money.tracker.bot.service.MessageService

@Configuration
class CategoryHandler(
    private val msgService: MessageService,
    private val categoryService: CategoryService,
) {
    companion object {
        const val CATEGORY_MANAGEMENT = "Управление категориями"

    }

    @Bean("categories_btn_text")
    fun categoriesBtn() = GeneralTextMessageHandler { msg ->
        if (msg.text != "Категории") return@GeneralTextMessageHandler

        msgService.send(
            CATEGORY_MANAGEMENT,
            MsgKeyboard().row()
                .button("Доход", "root_categories", CashFlowType.INCOME.name)
                .button("Расход", "root_categories", CashFlowType.OUTCOME.name)
        )
    }

    @Bean("categories_btn_clbk")
    fun categoriesClbk() = CallbackHandler { clbk ->
        ContextHolder.current.remove(clbk.chatId)
        msgService.edit(
            clbk.msgId,
            CATEGORY_MANAGEMENT,
            MsgKeyboard().row()
                .button("Доход", "root_categories", CashFlowType.INCOME.name)
                .button("Расход", "root_categories", CashFlowType.OUTCOME.name)
        )
    }

    @Bean("root_categories")
    fun categoriesInfoBtn() = CallbackHandler { clbk ->
        rootCategories(clbk.chatId, CashFlowType.valueOf(clbk.data), clbk.userId, clbk.msgId)
    }

    private fun rootCategories(chatId: Long, cashFlowType: CashFlowType, userId: Long, msgId: Int) {
        ContextHolder.current.remove(chatId)

        val s = when (cashFlowType) {
            CashFlowType.INCOME -> "дохода"
            CashFlowType.OUTCOME -> "расхода"
        }

        val keyboard = categoryService
            .getRootCategoriesKeyboard(userId, cashFlowType, "subcategories")
            .row()
            .button("<< Назад", "categories_btn_clbk")
            .button("+ Добавить", "add_root_category", cashFlowType.name)
        msgService.edit(msgId, "Управление категориями $s", keyboard)
    }

    @Bean("subcategories")
    fun subcategories() = CallbackHandler { clbk ->
        subcategories(clbk.msgId, clbk.data.toLong())

    }

    private fun subcategories(msgId: Int, categoryId: Long) {
        ContextHolder.removeContext()
        val category = categoryService.findById(categoryId)
        val subcategoriesKeyboard = categoryService
            .getSubcategoriesKeyboard(categoryId, "subcategory")
            .row()
            .button("Удалить", "delete_category", categoryId.toString())
            .row()
            .button("<< Назад", "root_categories", category.type!!.name)
            .button("+ Добавить", "add_subcategory", categoryId.toString())

        msgService.edit(msgId, "Управление категорией \"${category.name}\"", subcategoriesKeyboard)
    }

    @Bean("subcategory")
    fun subcategory() = CallbackHandler { clbk ->
        ContextHolder.removeContext()
        val categoryIdStr = clbk.data.substringBefore(':')
        val categoryId = categoryIdStr.toLong()
        val category = categoryService.findById(categoryId)

        val keyboard = MsgKeyboard()
            .row()
            .button("Удалить", "delete_category", categoryIdStr)
            .row()
            .button("<< Назад", "subcategories", category.parenCategory!!.id.toString())
        msgService.edit(clbk.msgId, "Управление подкатегорией \"${category.name}\"", keyboard)

    }

    @Bean("add_root_category")
    fun addRootCategory() = CallbackHandler { clbk ->
        ContextHolder.removeContext()
        msgService.edit(
            clbk.msgId,
            "Введите название категории",
            MsgKeyboard().row().button("<< Назад", "root_categories", clbk.data)
        )
        ContextHolder.current[clbk.chatId] =
            NewCategoryContext(clbk.msgId, "category_name_input", CashFlowType.valueOf(clbk.data))
    }

    @Bean("add_subcategory")
    fun addSubcategory() = CallbackHandler { clbk ->
        ContextHolder.removeContext()
        val category = categoryService.findById(clbk.data.toLong())

        msgService.edit(
            clbk.msgId,
            "Введите название подкатегории для категории ${category.name}",
            MsgKeyboard().row().button("<< Назад", "subcategories", category.id.toString())
        )
        ContextHolder.current[clbk.chatId] =
            NewCategoryContext(
                clbk.msgId,
                "subcategory_name_input",
                category.type,
                parentCategory = category.id
            )
    }

    @Bean("subcategory_name_input")
    fun subcategoryNameInput() = ContextualTextMessageHandler { msg ->
        val context: NewCategoryContext = ContextHolder.current()!!
        context.name = msg.text
        msgService.delete(msg.msgId)
        msgService.edit(
            context.baseMsgId, "Название новой подкатегории: \"${msg.text}\"", MsgKeyboard().row()
                .button("<< Назад", "subcategories", context.parentCategory.toString())
                .button("Подтвердить", "save_category")
        )

    }

    @Bean("category_name_input")
    fun categoryNameInput() = ContextualTextMessageHandler { msg ->
        val context: NewCategoryContext = ContextHolder.current()!!
        context.name = msg.text
        msgService.delete(msg.msgId)
        msgService.edit(
            context.baseMsgId, "Название новой категории: \"${msg.text}\"", MsgKeyboard().row()
                .button("<< Назад", "root_categories", context.type!!.name)
                .button("Подтвердить", "save_category")
        )
    }

    @Bean("save_category")
    fun saveCategory() = CallbackHandler { clbk ->
        val context: NewCategoryContext = ContextHolder.current()!!

        categoryService.save(clbk.userId, context.name!!, context.type!!, context.parentCategory)

        if (context.parentCategory != null) {
            subcategories(clbk.msgId, context.parentCategory!!)
        } else {
            rootCategories(clbk.chatId, context.type!!, clbk.userId, clbk.msgId)
        }


    }

    @Bean("delete_category")
    fun deleteCategory() = CallbackHandler { clbk ->
        val categoryId = clbk.data.toLong()

        val category = categoryService.findById(categoryId)
        val parentCategoryId = category.parenCategory?.id

        val subcategories = categoryService.findByParentId(categoryId)

        if (subcategories.isNotEmpty()) {
            subcategories.forEach {
                categoryService.delete(it.id!!)
            }
        }

        categoryService.delete(categoryId)

        if (parentCategoryId != null) {
            subcategories(clbk.msgId, parentCategoryId)
        } else {
            rootCategories(clbk.chatId, category.type!!, clbk.userId, clbk.msgId)
        }


    }


}


