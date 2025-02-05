package ru.andchelp.money.tracker.bot.handler

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.andchelp.money.tracker.bot.config.TextKey
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
import ru.andchelp.money.tracker.bot.service.UserService
import java.math.BigDecimal
import java.time.LocalDate

@Configuration
class OperationHandler(
    private val msgService: MessageService,
    private val categoryService: CategoryService,
    private val accountService: AccountService,
    private val operationService: OperationService,
    private val userService: UserService
) {
    companion object {
        const val NOT_SELECTED = "не указано"
    }

    @Bean("new_operation")
    fun newOutcomeOperation() = GeneralTextMessageHandler { msg ->
        if (!(msg.text == TextKey.OUTCOME || msg.text == TextKey.INCOME)) return@GeneralTextMessageHandler
        val flowType = when (msg.text) {
            TextKey.OUTCOME -> CashFlowType.OUTCOME
            TextKey.INCOME -> CashFlowType.INCOME
            else -> throw RuntimeException()
        }
        val operation = Operation(type = flowType)
        val text = "Добавление операции ${if (flowType == CashFlowType.OUTCOME) "расхода" else "дохода"}"
        val message = msgService.send(text, operationInputKeyboard(operation))
        ContextHolder.current[msg.chatId] = NewOperationContext(message.messageId, null, operation)
    }

    private fun operationInputKeyboard(operation: Operation) = MsgKeyboard()
        .row().button("🔢 Сумма: ${operation.sum ?: NOT_SELECTED}", "new_operation_sum")
        .row().button("📆 Дата: ${operation.date.toLocalDate()}", "new_operation_date")
        .row().button("🗂 Категория: ${operation.category?.name ?: NOT_SELECTED}", "new_operation_category")
        .row().button("💼 Счет: ${operation.account?.name ?: NOT_SELECTED}", "new_operation_account")
        .row().button(TextKey.CANCEL, "cancel_operation_creation")

    @Bean("new_operation_clbk")
    fun newOutcomeOperationClbk() = CallbackHandler { clbk ->

        val context: NewOperationContext = ContextHolder.current()!!
        context.handlerId = null

        refreshOperationMessage(clbk.msgId, context.operation)

    }

    private fun refreshOperationMessage(msgId: Int, operation: Operation) {
        val keyboard = operationInputKeyboard(operation)

        if (operation.sum != null && operation.category?.name != null && operation.account?.name != null) {
            keyboard.row().button(TextKey.CONFIRM, "create_new_operation")
        }
        val text = "Добавление операции ${if (operation.type!! == CashFlowType.OUTCOME) "расхода" else "дохода"}"
        msgService.edit(msgId, text, keyboard)
    }

    @Bean("cancel_operation_creation")
    fun cancelOperationCreation() = CallbackHandler { clbk ->
        msgService.delete(clbk.msgId)
        ContextHolder.removeContext()
    }

    @Bean("new_operation_sum")
    fun newOperationSum() = CallbackHandler { clbk ->
        msgService.edit(clbk.msgId, "Введите сумму", MsgKeyboard().row().button(TextKey.BACK, "new_operation_clbk"))

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
            MsgKeyboard().row().button(TextKey.BACK, "new_operation_clbk")
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
                context.operation.type!!,
                "category_for_new_operation"
            ).row().button(TextKey.BACK, "new_operation_clbk")
        )
    }

    @Bean("category_for_new_operation")
    fun categoryForNewOperation() = CallbackHandler { clbk ->
        msgService.edit(
            clbk.msgId,
            "Подтвердите или выберите подкатегорию",
            categoryService.getSubcategoriesKeyboard(clbk.data.toLong(), "subcategory_for_new_operation")
                .row().button(TextKey.CONFIRM, "set_category_for_new_operation", clbk.data)
                .row().button(TextKey.BACK, "new_operation_category")
        )
    }

    @Bean("subcategory_for_new_operation")
    fun subcategoryForNewOperation() = CallbackHandler { clbk ->
        msgService.edit(
            clbk.msgId,
            "Подтвердите выбор подкатегории",
            MsgKeyboard()
                .row().button(TextKey.CONFIRM, "set_category_for_new_operation", clbk.data)
                .row().button(TextKey.BACK, "new_operation_category")
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
            ).row().button(TextKey.BACK, "new_operation_clbk")
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
        operation.user = userService.findById(clbk.userId)
        operationService.save(operation)

        msgService.edit(
            clbk.msgId, "Добавлена новая операция:\n" +
                    "${operation.account!!.name}, ${operation.category!!.name}, " +
                    (if (operation.type == CashFlowType.INCOME) "+" else "-") +
                    "${operation.sum} ${operation.account!!.currency!!.symbol}"
        )
        ContextHolder.removeContext()
    }
}