package ru.andchelp.money.tracker.bot.infra

import ru.andchelp.money.tracker.bot.model.Operation
import ru.andchelp.money.tracker.bot.service.OperationFilter
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

class ContextHolder {

    companion object {
        val chatId: ThreadLocal<Long> = ThreadLocal()
        val current: ConcurrentHashMap<Long, Context> = ConcurrentHashMap()

        fun removeContext() {
            current.remove(chatId.get())
        }

        inline fun <reified T : Context> current(): T? {
            val context = current[chatId.get()]
            if (context is T) {
                return context
            }
            return null
        }
    }

}


open class Context(val baseMsgId: Int, var handlerId: String?)

class GreetingNewAccountContext(
    baseMsgId: Int,
    handlerId: String? = null,
    var currency: String? = null,
    var name: String? = null
) : Context(baseMsgId, handlerId)

class EditAccountContext(
    baseMsgId: Int,
    handlerId: String? = null,
    var accountId: Long? = null,
    var currency: String? = null,
    var name: String? = null
) : Context(baseMsgId, handlerId)

class NewAccountContext(
    baseMsgId: Int,
    handlerId: String? = null,
    var currency: String? = null,
    var name: String? = null
) : Context(baseMsgId, handlerId)

class NewCategoryContext(
    baseMsgId: Int,
    handlerId: String? = null,
    var type: CashFlowType? = null,
    var name: String? = null,
    var parentCategory: Long? = null
) : Context(baseMsgId, handlerId)

class NewOperationContext(
    baseMsgId: Int,
    handlerId: String? = null,
    var operation: Operation
) : Context(baseMsgId, handlerId)

class OperationFilterContext(
    baseMsgId: Int,
    handlerId: String? = null,
    val operationFilter: OperationFilter
) : Context(baseMsgId, handlerId)

class CategoryReportContext(
    baseMsgId: Int,
    handlerId: String? = null,
    var dateStart: LocalDate = LocalDate.now().withDayOfMonth(1),
    var dateEnd: LocalDate = LocalDate.now(),
    val accountIds: MutableSet<Long> = mutableSetOf()
) : Context(baseMsgId, handlerId)