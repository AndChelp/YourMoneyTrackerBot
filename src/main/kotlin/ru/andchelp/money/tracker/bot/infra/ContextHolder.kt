package ru.andchelp.money.tracker.bot.infra

import java.util.concurrent.ConcurrentHashMap

class ContextHolder {
    companion object {
        val current: ConcurrentHashMap<Long, Context> = ConcurrentHashMap()
    }
}

interface Context

class GreetingContext : Context
data class GreetingNewAccountContext(val baseMsgId: Int, var currency: String? = null, var name: String? = null) : Context
data class NewAccountContext(val baseMsgId: Int, var currency: String? = null, var name: String? = null) : Context
