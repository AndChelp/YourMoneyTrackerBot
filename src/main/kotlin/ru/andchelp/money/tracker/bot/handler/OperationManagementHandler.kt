package ru.andchelp.money.tracker.bot.handler

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.andchelp.money.tracker.bot.config.TextKey
import ru.andchelp.money.tracker.bot.handler.type.CallbackHandler
import ru.andchelp.money.tracker.bot.infra.MsgKeyboard
import ru.andchelp.money.tracker.bot.service.AccountService
import ru.andchelp.money.tracker.bot.service.CategoryService
import ru.andchelp.money.tracker.bot.service.MessageService
import ru.andchelp.money.tracker.bot.service.OperationService

@Configuration
class OperationManagementHandler(
    private val msgService: MessageService,
    private val categoryService: CategoryService,
    private val accountService: AccountService,
    private val operationService: OperationService
) {

    @Bean("manage_operation")
    fun manageOperation() = CallbackHandler { clbk ->
        val operation = operationService.findById(clbk.data.toLong())

        msgService.edit(
            clbk.msgId,
            "Изменение операции",
            MsgKeyboard()
                .row().button("💼 Счет: ${operation.account!!.name}", "manage_operation_account")
                .row().button("🗂 Категория: ${operation.category!!.name}", "manage_operation_category")
                .row().button("📆 Дата: ${operation.date.toLocalDate()}", "manage_operation_date")
                .row()
                .button("🔢 Сумма: ${operation.sum}${operation.account!!.currency!!.symbol}", "manage_operation_sum")
                .row().button("🔄 Повторение: ${operation.repeatFrequency ?: "-"}", "manage_operation_repeat")
                .row().button(TextKey.DELETE, "manage_operation_deletion")
                .row().button(TextKey.BACK, "operation_history_page", 0)
        )

    }

}