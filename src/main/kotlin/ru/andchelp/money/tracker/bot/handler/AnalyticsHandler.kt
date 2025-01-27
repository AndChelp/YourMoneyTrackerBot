package ru.andchelp.money.tracker.bot.handler

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.andchelp.money.tracker.bot.abbreviate
import ru.andchelp.money.tracker.bot.handler.type.CallbackHandler
import ru.andchelp.money.tracker.bot.handler.type.GeneralTextMessageHandler
import ru.andchelp.money.tracker.bot.infra.CategoryReportContext
import ru.andchelp.money.tracker.bot.infra.ContextHolder
import ru.andchelp.money.tracker.bot.infra.MsgKeyboard
import ru.andchelp.money.tracker.bot.service.AccountService
import ru.andchelp.money.tracker.bot.service.MessageService
import ru.andchelp.money.tracker.bot.toggleItem
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Configuration
class AnalyticsHandler(
    private val msgService: MessageService,
    private val accountService: AccountService
) {

    @Bean("reports")
    fun reports() = GeneralTextMessageHandler { msg ->
        if (msg.text != "Аналитика") return@GeneralTextMessageHandler
        val keyboard = MsgKeyboard()
        SupportedReports.entries.map {
            keyboard.row().button(it.text, it.clbkId)
        }
        msgService.send("Выберите тип отчета", keyboard)
    }

    @Bean("reports_clbk")
    fun reportsClbk() = CallbackHandler { clbk ->
        val keyboard = MsgKeyboard()
        SupportedReports.entries.map {
            keyboard.row().button(it.text, it.clbkId)
        }
        msgService.edit(clbk.msgId, "Выберите тип отчета", keyboard)
    }

    @Bean("accounts_report")
    fun accountsReport() = CallbackHandler { clbk ->
        var context: CategoryReportContext? = ContextHolder.current()
        if (context == null) {
            context = CategoryReportContext(clbk.msgId)
            ContextHolder.current[clbk.chatId] = context
        }

        msgService.edit(
            clbk.msgId,
            "Укажите параметры отчета",
            MsgKeyboard()
                .row()
                .button("С: ${context.dateStart}", "acc_rep_input_start_date", context.dateStart)
                .button("По: ${context.dateEnd}", "acc_rep_input_end_date", context.dateStart)
                .row().button(
                    "Счета: ${
                        accountService.findByIds(context.accountIds).joinToString(", ") { it.name!!.abbreviate() }
                            .takeIf { it.isNotEmpty() } ?: "все"
                    }", "acc_rep_select_accounts")
                .row().button("<< Назад", "reports_clbk").button("Показать", "show_accounts_report")
        )
    }

    @Bean("show_accounts_report")
    fun showAccountsReport() = CallbackHandler { clbk ->

        msgService.edit(
            clbk.msgId, "Сводный отчет по счетам за\n" +
                    "24.11.2024 - 30.11.2024\n" +
                    "\n" +
                    "Счет: Зарплатная карта\n" +
                    "Доходы: 600₽\n" +
                    "Расходы: 300₽\n" +
                    "\n" +
                    "Счет: Наличка usd\n" +
                    "Доходы: 0\$\n" +
                    "Расходы: 3\$", MsgKeyboard().row().button("<< Назад", "accounts_report")
        )

    }

    @Bean("acc_rep_select_accounts")
    fun accRepSelectAccounts() = CallbackHandler { clbk ->
        val context: CategoryReportContext = ContextHolder.current()!!
        val accountIds = context.accountIds
        if (clbk.data.isNotEmpty())
            accountIds.toggleItem(clbk.data.toLong())

        val keyboard = accountService.getKeyboard(clbk.userId, "acc_rep_select_accounts")
        keyboard.keyboard.forEach { row ->
            row.forEach { button ->
                val status = if (accountIds.contains(button.callbackData.substringAfter(":").toLong()))
                    "вкл" else "выкл"
                button.text = "($status) ${button.text}"
            }
        }
        keyboard.row().button("Применить", "accounts_report")
        msgService.edit(
            clbk.msgId,
            "Выберите счета для отчета:",
            keyboard
        )
    }

    @Bean("acc_rep_input_start_date")
    fun inputAccountsReportStartPeriod() = CallbackHandler { clbk ->
        val date = LocalDate.parse(clbk.data)

        val context: CategoryReportContext = ContextHolder.current()!!
        context.dateStart = date
        val keyboard = MsgKeyboard()
            .row().button("Изменить", "date_input", "acc_rep_input_start_date:${clbk.data}")
            .row().button("Готово", "accounts_report")
        msgService.edit(clbk.msgId, "Начало периода отчета: $date", keyboard)
    }


    @Bean("acc_rep_input_end_date")
    fun inputAccountsReportEndPeriod() = CallbackHandler { clbk ->
        val date = LocalDate.parse(clbk.data)

        val context: CategoryReportContext = ContextHolder.current()!!
        context.dateEnd = date
        val keyboard = MsgKeyboard()
            .row().button("Изменить", "date_input", "acc_rep_input_end_date:${clbk.data}")
            .row().button("Готово", "accounts_report")
        msgService.edit(clbk.msgId, "Окончание периода отчета: $date", keyboard)
    }

    @Bean("date_input")
    fun dateInput() = CallbackHandler { clbk ->
        val callback = clbk.data.substringBefore(":")
        val date = LocalDate.parse(clbk.data.substringAfter(":"))
        val keyboard = keyboardForDate(date, callback)
            .row().button("Отменить", "accounts_report")
            .button("Готово", callback, date)
        msgService.edit(clbk.msgId, "Введите дату нажимая кнопки", keyboard)
    }

    @Bean("change_day")
    fun changeDay() = CallbackHandler { clbk ->
        val callback = clbk.data.substringBefore(":")
        val date = LocalDate.parse(clbk.data.substringAfter(":"))

        val keyboard = keyboardForDate(date, callback)
        val message = clbk.update.callbackQuery.message as Message
        val clbkKeyboard = message.replyMarkup.keyboard
        if (clbkKeyboard.size < 3 || !clbkKeyboard[3][0].callbackData.startsWith("change_day")) {
            (1..YearMonth.from(date).lengthOfMonth()).chunked(7).forEach { row ->
                keyboard.row()
                row.forEach { keyboard.button(it.toString(), "change_day", "$callback:${date.withDayOfMonth(it)}") }
            }
        }

        keyboard.row()
            .button("Отменить", "accounts_report")
            .button("Готово", callback, date)
        msgService.edit(clbk.msgId, "Введите дату нажимая кнопки", keyboard)
    }

    @Bean("change_month")
    fun changeMonth() = CallbackHandler { clbk ->
        val callback = clbk.data.substringBefore(":")
        val date = LocalDate.parse(clbk.data.substringAfter(":"))

        val keyboard = keyboardForDate(date, callback)
        val message = clbk.update.callbackQuery.message as Message
        val clbkKeyboard = message.replyMarkup.keyboard
        if (clbkKeyboard.size < 3 || !clbkKeyboard[3][0].callbackData.startsWith("change_month")) {
            Month.entries.chunked(4).forEach { row ->
                keyboard.row()
                row.forEach {
                    keyboard.button(
                        it.getDisplayName(TextStyle.SHORT_STANDALONE, Locale.forLanguageTag("RU")),
                        "change_month",
                        "$callback:${date.withMonth(it.value)}"
                    )
                }
            }
        }

        keyboard.row()
            .button("Отменить", "accounts_report")
            .button("Готово", callback, date)
        msgService.edit(clbk.msgId, "Введите дату нажимая кнопки", keyboard)
    }

    @Bean("change_year")
    fun changeYear() = CallbackHandler { clbk ->
        val pageFlip = clbk.data.startsWith("!")
        val substringBefore = clbk.data.substringBefore(":")
        val callback = if (pageFlip) substringBefore.substring(1) else substringBefore
        val date = LocalDate.parse(clbk.data.substringAfter(":"))

        val keyboard = keyboardForDate(date, callback)
        val message = clbk.update.callbackQuery.message as Message
        val clbkKeyboard = message.replyMarkup.keyboard
        if (pageFlip || clbkKeyboard.size < 3 || !clbkKeyboard[3][0].callbackData.startsWith("change_year")) {
            val year = date.year

            val before = (year - 9)..(year - 2)
            keyboard.row()
            before.forEach { keyboard.button(it.mod(100), "change_year", "$callback:${date.withYear(it)}") }

            val now = (year - 1)..(year + 1)
            keyboard.row()
            now.forEach { keyboard.button(it, "change_year", "$callback:${date.withYear(it)}") }

            val after = (year + 2)..(year + 9)
            keyboard.row()
            after.forEach { keyboard.button(it.mod(100), "change_year", "$callback:${date.withYear(it)}") }

            keyboard.row()
                .button("<<", "change_year", "!$callback:${date.withYear(year - 19)}")
                .button(">>", "change_year", "!$callback:${date.withYear(year + 19)}")
        }

        keyboard.row()
            .button("Отменить", "accounts_report")
            .button("Готово", callback, date)
        msgService.edit(clbk.msgId, "Введите дату нажимая кнопки", keyboard)
    }


    fun keyboardForDate(date: LocalDate, callback: String): MsgKeyboard {
        val keyboard = MsgKeyboard().row()
        keyboard.button(date.dayOfMonth.toString(), "change_day", "$callback:$date")
        keyboard.button(
            date.month.getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("RU")),
            "change_month",
            "$callback:$date"
        )
        keyboard.button(date.year.toString(), "change_year", "$callback:$date")
        return keyboard
    }

}

enum class SupportedReports(val text: String, val clbkId: String) {
    CATEGORY("Сравнительный по категориям", "category_report"),
    ACCOUNTS("Сводный по счетам", "accounts_report"),
}