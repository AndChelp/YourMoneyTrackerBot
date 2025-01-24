package ru.andchelp.money.tracker.bot.infra

import java.util.concurrent.ConcurrentHashMap

class ContextHolder {

    companion object {
        val chatId: ThreadLocal<Long> = ThreadLocal()
        val current: ConcurrentHashMap<Long, Context> = ConcurrentHashMap()

        inline fun <reified T : Context> current(): T? {
            val context = current[chatId.get()]
            if (context is T) {
                return context
            }
            return null
        }
    }

}


interface Context

class GreetingContext : Context
data class GreetingNewAccountContext(val baseMsgId: Int, var currency: String? = null, var name: String? = null) :
    Context

data class NewAccountContext(val baseMsgId: Int, var currency: String? = null, var name: String? = null) : Context
