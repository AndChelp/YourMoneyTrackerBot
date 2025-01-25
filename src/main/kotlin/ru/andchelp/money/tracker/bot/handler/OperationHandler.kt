package ru.andchelp.money.tracker.bot.handler

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.andchelp.money.tracker.bot.handler.type.CallbackHandler
import ru.andchelp.money.tracker.bot.handler.type.ContextualTextMessageHandler
import ru.andchelp.money.tracker.bot.handler.type.GeneralTextMessageHandler
import ru.andchelp.money.tracker.bot.infra.CashFlowType
import ru.andchelp.money.tracker.bot.infra.ContextHolder
import ru.andchelp.money.tracker.bot.infra.MsgKeyboard
import ru.andchelp.money.tracker.bot.infra.NewOperationContext
import ru.andchelp.money.tracker.bot.model.Operation
import ru.andchelp.money.tracker.bot.service.AccountService
import ru.andchelp.money.tracker.bot.service.CategoryService
import ru.andchelp.money.tracker.bot.service.MessageService
import ru.andchelp.money.tracker.bot.service.OperationService
import java.math.BigDecimal
import java.time.LocalDate

@Configuration
class OperationHandler(
    private val msgService: MessageService,
    private val categoryService: CategoryService,
    private val accountService: AccountService,
    private val operationService: OperationService
) {
    @Bean("new_operation")
    fun newOutcomeOperation() = GeneralTextMessageHandler { msg ->
        if (!(msg.text == "Расход" || msg.text == "Доход")) return@GeneralTextMessageHandler
        val flowType = when (msg.text) {
            "Расход" -> CashFlowType.OUTCOME
            "Доход" -> CashFlowType.INCOME
            else -> throw RuntimeException()
        }
        val message = msgService.send(
            "Добавление операции ${
                when (flowType) {
                    CashFlowType.OUTCOME -> "расхода"
                    CashFlowType.INCOME -> "дохода"
                }
            }",
            MsgKeyboard()
                .row().button("Сумма: не указано", "new_operation_sum")
                .row().button("Дата: ${LocalDate.now()}", "new_operation_date")
                .row().button("Категория: не указано", "new_operation_category")
                .row().button("Счет: не указано", "new_operation_account")
                .row().button("Отмена", "cancel_operation_creation")
        )
        ContextHolder.current[msg.chatId] = NewOperationContext(message.messageId, null, Operation(type = flowType))
    }

    @Bean("new_operation_clbk")
    fun newOutcomeOperationClbk() = CallbackHandler { clbk ->

        val context: NewOperationContext = ContextHolder.current()!!
        context.handlerId = null

        refreshOperationMessage(clbk.msgId, context.operation)

    }

    private fun refreshOperationMessage(
        msgId: Int, operation: Operation
    ) {
        val NOT_SELECTED = "не указано"
        val keyboard = MsgKeyboard()
            .row().button("Сумма: ${operation.sum ?: NOT_SELECTED}", "new_operation_sum")
            .row().button("Дата: ${operation.date.toLocalDate()}", "new_operation_date")
            .row().button("Категория: ${operation.category?.name ?: NOT_SELECTED}", "new_operation_category")
            .row().button("Счет: ${operation.account?.name ?: NOT_SELECTED}", "new_operation_account")
            .row().button("Отмена", "cancel_operation_creation")

        if (operation.sum != null && operation.category?.name != null && operation.account?.name != null) {
            keyboard.row().button("Подтвердить", "create_new_operation")
        }
        msgService.edit(
            msgId,
            "Добавление операции ${
                when (operation.type) {
                    CashFlowType.OUTCOME -> "расхода"
                    CashFlowType.INCOME -> "дохода"
                }
            }",
            keyboard
        )
    }

    @Bean("cancel_operation_creation")
    fun cancelOperationCreation() = CallbackHandler { clbk ->
        msgService.delete(clbk.msgId)
        ContextHolder.removeContext()
    }

    @Bean("new_operation_sum")
    fun newOperationSum() = CallbackHandler { clbk ->
        msgService.edit(clbk.msgId, "Введите сумму", MsgKeyboard().row().button("Назад", "new_operation_clbk"))

        val context: NewOperationContext = ContextHolder.current()!!
        context.handlerId = "new_operation_sum_input"
    }

    @Bean("new_operation_sum_input")
    fun newOperationSumInput() = ContextualTextMessageHandler { msg ->
        val context: NewOperationContext = ContextHolder.current()!!
        context.handlerId = null
        context.operation.sum = BigDecimal(msg.text)

        refreshOperationMessage(context.baseMsgId, context.operation)
        msgService.delete(msg.msgId)
    }

    @Bean("new_operation_date")
    fun newOperationDate() = CallbackHandler { clbk ->
        msgService.edit(
            clbk.msgId,
            "Введите дату в формате ГГГГ-ММ-ДД",
            MsgKeyboard().row().button("Назад", "new_operation_clbk")
        )

        val context: NewOperationContext = ContextHolder.current()!!
        context.handlerId = "new_operation_date_input"
    }

    @Bean("new_operation_date_input")
    fun newOperationDateInput() = ContextualTextMessageHandler { msg ->
        val context: NewOperationContext = ContextHolder.current()!!
        context.handlerId = null
        context.operation.date = LocalDate.parse(msg.text).atStartOfDay()

        refreshOperationMessage(context.baseMsgId, context.operation)
        msgService.delete(msg.msgId)
    }

    @Bean("new_operation_category")
    fun newOperationCategory() = CallbackHandler { clbk ->
        val context: NewOperationContext = ContextHolder.current()!!
        msgService.edit(
            clbk.msgId,
            "Выберите категорию или подкатегорию операции",
            categoryService.getRootCategoriesKeyboard(
                clbk.userId,
                context.operation.type,
                "category_for_new_operation"
            ).row().button("Назад", "new_operation_clbk")
        )
    }

    @Bean("category_for_new_operation")
    fun categoryForNewOperation() = CallbackHandler { clbk ->
        msgService.edit(
            clbk.msgId,
            "Подтвердите или выберите подкатегорию",
            categoryService.getSubcategoriesKeyboard(clbk.data.toLong(), "subcategory_for_new_operation")
                .row().button("Выбрать", "set_category_for_new_operation", clbk.data)
                .row().button("Назад", "new_operation_category")
        )
    }

    @Bean("subcategory_for_new_operation")
    fun subcategoryForNewOperation() = CallbackHandler { clbk ->
        msgService.edit(
            clbk.msgId,
            "Подтвердите выбор подкатегории",
            MsgKeyboard()
                .row().button("Выбрать", "set_category_for_new_operation", clbk.data)
                .row().button("Назад", "new_operation_category")
        )
    }

    @Bean("set_category_for_new_operation")
    fun setCategoryForNewOperation() = CallbackHandler { clbk ->
        val context: NewOperationContext = ContextHolder.current()!!
        val category = categoryService.findById(clbk.data.toLong())
        context.operation.category = category
        refreshOperationMessage(clbk.msgId, context.operation)
    }

    @Bean("new_operation_account")
    fun newOprationAccount() = CallbackHandler { clbk ->
        msgService.edit(
            clbk.msgId,
            "Выберите счет для операции",
            accountService.getKeyboard(
                clbk.userId,
                "account_for_new_operation"
            ).row().button("Назад", "new_operation_clbk")
        )
    }

    @Bean("account_for_new_operation")
    fun accountForNewOperation() = CallbackHandler { clbk ->
        val context: NewOperationContext = ContextHolder.current()!!
        val account = accountService.findById(clbk.data.toLong())
        context.operation.account = account
        refreshOperationMessage(clbk.msgId, context.operation)
    }

    @Bean("create_new_operation")
    fun createNewOperation() = CallbackHandler { clbk ->
        val context: NewOperationContext = ContextHolder.current()!!
        val operation = context.operation
        operationService.save(operation)

        msgService.edit(
            clbk.msgId, "Добавлена новая операция:\n" +
                    "${operation.account!!.name}, ${operation.category!!.name}, " +
                    (if (operation.type == CashFlowType.INCOME) "+" else "-") +
                    "${operation.sum} ${operation.account!!.currencyCode}"
        )
        ContextHolder.removeContext()

    }
}