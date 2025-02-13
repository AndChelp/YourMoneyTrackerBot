package ru.andchelp.money.tracker.bot.config

import ru.andchelp.money.tracker.bot.infra.TgKeyboard

class MenuConfig {
    companion object {
        val SIMPLE = TgKeyboard().row().button(TextKey.SETTINGS).button(TextKey.HELP)
        val FULL = TgKeyboard()
            .row().button(TextKey.INCOME).button(TextKey.OUTCOME)
            .row().button(TextKey.ACCOUNTS).button(TextKey.CATEGORIES)
            .row().button(TextKey.ANALYTICS).button(TextKey.OPERATIONS)
            .row().button(TextKey.SHOPPING_LIST).button(TextKey.IMPORT)
            .row().button(TextKey.SETTINGS).button(TextKey.HELP)
    }
}