package ru.andchelp.money.tracker.bot.handler

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.andchelp.money.tracker.bot.config.TextKey
import ru.andchelp.money.tracker.bot.handler.type.CallbackHandler
import ru.andchelp.money.tracker.bot.handler.type.ContextualTextMessageHandler
import ru.andchelp.money.tracker.bot.handler.type.GeneralTextMessageHandler
import ru.andchelp.money.tracker.bot.infra.CashFlowType
import ru.andchelp.money.tracker.bot.infra.ContextHolder
import ru.andchelp.money.tracker.bot.infra.ImportOperationsContext
import ru.andchelp.money.tracker.bot.infra.MsgKeyboard
import ru.andchelp.money.tracker.bot.model.Operation
import ru.andchelp.money.tracker.bot.service.AccountService
import ru.andchelp.money.tracker.bot.service.CategoryService
import ru.andchelp.money.tracker.bot.service.MessageService
import ru.andchelp.money.tracker.bot.service.OperationService
import ru.andchelp.money.tracker.bot.service.UserService
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

@Configuration
class ImportHandler(
    private val msgService: MessageService,
    private val operationService: OperationService,
    private val accountService: AccountService,
    private val categoryService: CategoryService,
    private val userService: UserService
) {

    @Bean("import_btn_text")
    fun importBtn() = GeneralTextMessageHandler { msg ->
        if (msg.text != TextKey.IMPORT) return@GeneralTextMessageHandler

        val message = msgService.send(
            "Импорт операций\n\n" +
                    "Прикрепите файл или отправьте данные сообщением\n\n" +
                    "Узнать подробнее про импорт можно в меню \"${TextKey.HELP}\"",
            MsgKeyboard().row()
                .button(TextKey.CANCEL, "cancel_import")
        )
        ContextHolder.current[msg.chatId] = ImportOperationsContext(message.messageId, handlerId = "text_import")
    }

    @Bean("cancel_import")
    fun cancelImport() = CallbackHandler { clbk ->
        msgService.delete(clbk.msgId)
        ContextHolder.removeContext()
    }

    @Bean("text_import")
    fun textImport() = ContextualTextMessageHandler { msg ->
        val operations = msg.text.split("\n")
            .map {
                val op = it.split(",")
                Operation(
                    account = accountService.findByIdAndUserId(op[0].toLong(), msg.userId),
                    sum = BigDecimal(op[1]).setScale(2, RoundingMode.HALF_EVEN),
                    type = CashFlowType.entries[op[2].toInt()],
                    category = categoryService.findByNameAndUserId(op[3], msg.userId),
                    date = LocalDate.parse(op[4]).atStartOfDay(),
                    user = userService.findById(msg.userId)
                )
            }
        operations.forEach { operationService.save(it) }

        val context: ImportOperationsContext = ContextHolder.current()!!

        val opText = operations.joinToString("\n") {
            "${it.account!!.name}, ${it.category!!.name}, " +
                    (if (it.type == CashFlowType.INCOME) "+" else "-") +
                    "${it.sum} ${it.account!!.currency!!.symbol}"
        }
        msgService.edit(
            context.baseMsgId,
            "Импортированы операции:\n$opText",
        )
        msgService.delete(msg.msgId)
        context.handlerId = null
    }

}