package ru.andchelp.money.tracker.bot.handler

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.Pageable
import ru.andchelp.money.tracker.bot.abbreviate
import ru.andchelp.money.tracker.bot.handler.type.CallbackHandler
import ru.andchelp.money.tracker.bot.handler.type.ContextualTextMessageHandler
import ru.andchelp.money.tracker.bot.handler.type.GeneralTextMessageHandler
import ru.andchelp.money.tracker.bot.infra.CashFlowType
import ru.andchelp.money.tracker.bot.infra.ContextHolder
import ru.andchelp.money.tracker.bot.infra.MsgKeyboard
import ru.andchelp.money.tracker.bot.infra.OperationFilterContext
import ru.andchelp.money.tracker.bot.service.AccountService
import ru.andchelp.money.tracker.bot.service.CategoryService
import ru.andchelp.money.tracker.bot.service.MessageService
import ru.andchelp.money.tracker.bot.service.OperationFilter
import ru.andchelp.money.tracker.bot.service.OperationService
import ru.andchelp.money.tracker.bot.toggleItem
import java.math.BigDecimal
import java.time.LocalDate

@Configuration
class OperationHistoryHandler(
    private val msgService: MessageService,
    private val operationService: OperationService,
    private val accountService: AccountService,
    private val categoryService: CategoryService
) {

    @Bean("operation_history")
    fun operationHistory() = GeneralTextMessageHandler { msg ->
        if (msg.text != "Операции") return@GeneralTextMessageHandler
        ContextHolder.removeContext()
        renderHistoryMsg(page = 0, filter = OperationFilter(msg.userId))
    }

    @Bean("operation_history_page")
    fun prevOperationPage() = CallbackHandler { clbk ->
        val context: OperationFilterContext? = ContextHolder.current()
        context?.handlerId = null
        renderHistoryMsg(clbk.data.toInt(), clbk.msgId, context?.operationFilter ?: OperationFilter(clbk.userId))
    }

    @Bean("change_operation_filter")
    fun changeOperationFilter() = CallbackHandler { clbk ->

        var context: OperationFilterContext? = ContextHolder.current()
        if (context == null) {
            context = OperationFilterContext(clbk.msgId, operationFilter = OperationFilter(clbk.userId))
            ContextHolder.current[clbk.chatId] = context
        }
        renderHistoryFilter(context.operationFilter, clbk.msgId)
    }

    @Bean("operation_filter_change_sum_from")
    fun operationFilterChangeSumFrom() = CallbackHandler { clbk ->
        val context: OperationFilterContext = ContextHolder.current()!!
        context.handlerId = "operation_filter_input_sum_from"
        msgService.edit(
            clbk.msgId,
            "Введите начало диапазона сумм",
            MsgKeyboard().row().button("Очистить значение", "operation_filter_clean_sum_from")
        )
    }

    @Bean("operation_filter_clean_sum_from")
    fun operationFilterCleanSumFrom() = CallbackHandler { clbk ->
        val context: OperationFilterContext = ContextHolder.current()!!
        context.handlerId = null
        context.operationFilter.sumFrom = null
        renderHistoryFilter(context.operationFilter, clbk.msgId)
    }

    @Bean("operation_filter_input_sum_from")
    fun operationFilterInputSumFrom() = ContextualTextMessageHandler { msg ->
        val context: OperationFilterContext = ContextHolder.current()!!
        context.operationFilter.sumFrom = BigDecimal(msg.text)
        renderHistoryFilter(context.operationFilter, context.baseMsgId)
        msgService.delete(msg.msgId)
    }

    @Bean("operation_filter_change_sum_till")
    fun operationFilterChangeSumTill() = CallbackHandler { clbk ->
        val context: OperationFilterContext = ContextHolder.current()!!
        context.handlerId = "operation_filter_input_sum_till"
        msgService.edit(
            clbk.msgId,
            "Введите окончание диапазона сумм",
            MsgKeyboard().row().button("Очистить значение", "operation_filter_clean_sum_till")
        )
    }

    @Bean("operation_filter_input_sum_till")
    fun operationFilterInputSumTill() = ContextualTextMessageHandler { msg ->
        val context: OperationFilterContext = ContextHolder.current()!!
        context.operationFilter.sumTill = BigDecimal(msg.text)
        renderHistoryFilter(context.operationFilter, context.baseMsgId)
        msgService.delete(msg.msgId)
    }

    @Bean("operation_filter_clean_sum_till")
    fun operationFilterCleanSumTill() = CallbackHandler { clbk ->
        val context: OperationFilterContext = ContextHolder.current()!!
        context.handlerId = null
        context.operationFilter.sumTill = null
        renderHistoryFilter(context.operationFilter, clbk.msgId)
    }

    @Bean("operation_filer_change_categories")
    fun operationFilerChangeCategory() = CallbackHandler { clbk ->
        val context: OperationFilterContext = ContextHolder.current()!!
        val categoryIds = context.operationFilter.categoryIds
        if (clbk.data.isNotEmpty())
            categoryIds.toggleItem(clbk.data.toLong())

        val keyboard = categoryService.getAllCategoriesKeyboard(
            clbk.userId,
            "operation_filer_change_categories"
        )

        keyboard.keyboard.forEach { row ->
            row.forEach { button ->
                val status = if (categoryIds.contains(button.callbackData.substringAfter(":").toLong()))
                    "вкл" else "выкл"
                button.text = "($status) ${button.text}"
            }
        }

        keyboard.row().button("Применить", "change_operation_filter")
        msgService.edit(
            clbk.msgId,
            "Выберите категории для фильтра:",
            keyboard
        )
    }

    @Bean("operation_filer_change_accounts")
    fun operationFilerChangeAccounts() = CallbackHandler { clbk ->
        val context: OperationFilterContext = ContextHolder.current()!!
        val accountIds = context.operationFilter.accountIds
        if (clbk.data.isNotEmpty())
            accountIds.toggleItem(clbk.data.toLong())

        val keyboard = accountService.getKeyboard(clbk.userId, "operation_filer_change_accounts")

        keyboard.keyboard.forEach { row ->
            row.forEach { button ->
                val status = if (accountIds.contains(button.callbackData.substringAfter(":").toLong()))
                    "вкл" else "выкл"
                button.text = "($status) ${button.text}"
            }
        }

        keyboard.row().button("Применить", "change_operation_filter")
        msgService.edit(
            clbk.msgId,
            "Выберите счета для фильтра:",
            keyboard
        )
    }

    @Bean("operation_filer_change_date_from")
    fun operationFilterChangeDateFrom() = CallbackHandler { clbk ->
        val context: OperationFilterContext = ContextHolder.current()!!
        context.handlerId = "operation_filer_input_date_from"
        msgService.edit(clbk.msgId, "Введите начало периода в формате ГГГГ-ММ-ДД")
    }

    @Bean("operation_filer_input_date_from")
    fun operationFilerInputDateFrom() = ContextualTextMessageHandler { msg ->
        val context: OperationFilterContext = ContextHolder.current()!!
        context.operationFilter.dateFrom = LocalDate.parse(msg.text).atStartOfDay()
        renderHistoryFilter(context.operationFilter, context.baseMsgId)
        msgService.delete(msg.msgId)
    }

    @Bean("operation_filer_change_date_till")
    fun operationFilterChangeDateTill() = CallbackHandler { clbk ->
        val context: OperationFilterContext = ContextHolder.current()!!
        context.handlerId = "operation_filer_input_date_till"
        msgService.edit(clbk.msgId, "Введите конец периода в формате ГГГГ-ММ-ДД")
    }

    @Bean("operation_filer_input_date_till")
    fun operationFilerInputDateTill() = ContextualTextMessageHandler { msg ->
        val context: OperationFilterContext = ContextHolder.current()!!
        context.operationFilter.dateTill = LocalDate.parse(msg.text).atStartOfDay()
        renderHistoryFilter(context.operationFilter, context.baseMsgId)
        msgService.delete(msg.msgId)
    }

    @Bean("operation_filter_change_type")
    fun changeTypeOfOperationFilter() = CallbackHandler { clbk ->
        val context: OperationFilterContext = ContextHolder.current()!!
        val types = context.operationFilter.types
        if (clbk.data.isNotEmpty())
            types.toggleItem(CashFlowType.valueOf(clbk.data))

        msgService.edit(
            clbk.msgId,
            "Выберите тип для фильтра",
            MsgKeyboard().row()
                .button(
                    "Доход (${if (types.contains(CashFlowType.INCOME)) "вкл" else "выкл"})",
                    "operation_filter_change_type",
                    CashFlowType.INCOME.name
                )
                .button(
                    "Расход (${if (types.contains(CashFlowType.OUTCOME)) "вкл" else "выкл"})",
                    "operation_filter_change_type",
                    CashFlowType.OUTCOME.name
                )
                .row().button("Применить", "change_operation_filter")
        )
    }

    private fun renderHistoryMsg(page: Int, msgId: Int? = null, filter: OperationFilter) {
        val operations = operationService.find(filter, Pageable.ofSize(10).withPage(page))

        val keyboard = MsgKeyboard()
        operations.chunked(2).forEach { chunk ->
            keyboard.row()
            chunk.forEach {
                val text = buildString {
                    append(if (it.type == CashFlowType.INCOME) "+" else "-")
                    append(it.sum!!.stripTrailingZeros().toPlainString())
                    append(it.account!!.currency!!.symbol)
                    append(", ")
                    append(it.category!!.name!!.abbreviate())
                    append(", ")
                    append(it.account!!.name!!.abbreviate())
                }
                keyboard.button(text, "manage_operation", it.id)
            }
        }
        paginationButtons(keyboard, page, operations.totalPages, "operation_history_page")
        keyboard.row().button("Изменить фильтр", "change_operation_filter")
        val text = filterParamsText(filter, operations.totalElements)
        msgId
            ?.let { msgService.edit(it, text, keyboard) }
            ?: msgService.send(text, keyboard)
    }

    private fun filterParamsText(filter: OperationFilter, totalOperations: Long): String {
        return buildString {
            appendLine("История операций")
            if (filter.types.isNotEmpty()) {
                append("Типы операций: ")
                appendLine(filter.types.joinToString(", ") {
                    when (it) {
                        CashFlowType.INCOME -> "доход"
                        CashFlowType.OUTCOME -> "расход"
                    }
                })
            }
            appendLine("Период: с ${filter.dateFrom.toLocalDate()} по ${filter.dateTill.toLocalDate()}")
            if (filter.accountIds.isNotEmpty()) {
                append("Счета: ")
                appendLine(accountService.findByIds(filter.accountIds).map { it.name }.joinToString(", "))
            }
            if (filter.categoryIds.isNotEmpty()) {
                append("Категории: ")
                appendLine(categoryService.findByIds(filter.categoryIds).map { it.name }.joinToString(", "))
            }
            if (filter.sumFrom != null || filter.sumTill != null) {
                append("Сумма:")
                if (filter.sumFrom != null)
                    append(" от ${filter.sumFrom}")
                if (filter.sumTill != null)
                    append(" до ${filter.sumTill}")
                appendLine()
            }
            appendLine("Операций найдено: $totalOperations")
        }
    }

    private fun paginationButtons(keyboard: MsgKeyboard, page: Int, totalPages: Int, newPageClbkId: String) {
        if (totalPages < 2) return
        keyboard.row()
        if (page > 0)
            keyboard.button("<<< ${page}/$totalPages", newPageClbkId, page - 1)
        else
            keyboard.button("<<< $totalPages/$totalPages", newPageClbkId, totalPages - 1)

        keyboard.button("Стр. ${page + 1}/$totalPages", "no_action")

        if ((page + 1) < totalPages)
            keyboard.button("${page + 2}/$totalPages >>>", newPageClbkId, page + 1)
        else
            keyboard.button("1/$totalPages >>>", newPageClbkId, 0)
    }

    private fun renderHistoryFilter(filter: OperationFilter, msgId: Int) {
        msgService.edit(
            msgId,
            "Параметры фильтра операций", MsgKeyboard()
                .row().button(
                    "Тип: ${
                        filter.types.joinToString(", ") {
                            when (it) {
                                CashFlowType.INCOME -> "доход"
                                CashFlowType.OUTCOME -> "расход"
                            }
                        }.takeIf { it.isNotEmpty() } ?: "любой"
                    }", "operation_filter_change_type"
                )
                .row()
                .button("С: ${filter.dateFrom.toLocalDate()}", "operation_filer_change_date_from")
                .button("По: ${filter.dateTill.toLocalDate()}", "operation_filer_change_date_till")
                .row().button(
                    "Счета: ${
                        accountService.findByIds(filter.accountIds).joinToString(", ") { it.name!!.abbreviate() }
                            .takeIf { it.isNotEmpty() } ?: "все"
                    }",
                    "operation_filer_change_accounts"
                )
                .row().button(
                    "Категории: ${
                        categoryService.findByIds(filter.categoryIds).joinToString(", ") { it.name!!.abbreviate() }
                            .takeIf { it.isNotEmpty() } ?: "все"
                    }", "operation_filer_change_categories"
                )
                .row()
                .button("Сумма от: ${filter.sumFrom ?: "-"}", "operation_filter_change_sum_from")
                .button("Сумма до: ${filter.sumTill ?: "-"}", "operation_filter_change_sum_till")
                .row().button("Применить", "operation_history_page", 0)
        )
    }
}