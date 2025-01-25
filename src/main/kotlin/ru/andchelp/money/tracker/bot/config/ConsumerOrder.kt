package ru.andchelp.money.tracker.bot.config

class ConsumerOrder {
    // the lower value, the higher order
    companion object {

        const val COMMAND = 777777
        const val TEXT_MESSAGE = 888888
        const val GENERAL_TEXT_MESSAGE = 888800
        const val CALLBACK = 999999

        const val DEFAULT = Int.MAX_VALUE
    }
}
