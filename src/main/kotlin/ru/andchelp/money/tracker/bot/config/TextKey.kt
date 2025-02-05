package ru.andchelp.money.tracker.bot.config

class TextKey {
    companion object {
        const val YOU_DONT_HAVE_ACC_CREATE_NEW = "У вас еще нет счетов!\nДля создания нового счета нажмите кнопку ниже"
        const val GLOBAL_BALANCE_YOUR_ACCS = "Общий баланс: %s%s\nВаши счета:"

        const val NEW_ACCOUNT = "🆕 Новый счет"
        const val WRITE_ACC_NAME = "Введите название счета"

        const val ACC_NAME_CHOOSE_CURRENCY = "Название счета: %s\nВыберите валюту счета"
        const val SCHOOSED_NAME_AND_CURRENCY = "Название счета: %s\nВалюта: %s"

        const val USE_MENU_FOR_NAVIGATION = "Используйте кнопки меню для навигации"
        const val SELECT_CURRENCY = "greeting.please.select.currency"
        const val FEW_STEPS = "greeting.few.steps"
        const val LAST_STEP_CREATE_NEW_ACC = "Еще один шаг - создайте первый счет"
        const val GLOBAL_CURRENCY = "Основная валюта:"
        const val END_GREETING_TEXT = "Ваш первый счет создан, теперь можно приступать к работе!\n" +
                "Можете начать с создания дополнительных счетов, настроить категории под себя или сразу приступить к " +
                "вводу доходов и расходов, используя соответствующие кнопки меню"


        // action buttons
        const val BACK = "↩️ Назад"
        const val CONFIRM = "✅ Подтвердить"
        const val APPLY = "✅ Применить"
        const val EDIT = "✏️ Изменить"
        const val DELETE = "❌ Удалить"
        const val CANCEL = "🚫 Отменить"
        const val ADD = "🆕 Добавить"
        const val CLEAN = "🗑 Очистить"

        //Menu buttons
        const val INCOME = "💰 Доход"
        const val OUTCOME = "💸 Расход"
        const val ACCOUNTS = "💼 Счета"
        const val CATEGORIES = "🗂 Категории"
        const val ANALYTICS = "📊 Аналитика"
        const val OPERATIONS = "⌛ Операции"
        const val SETTINGS = "⚙️ Настройки"
        const val HELP = "ℹ️ Помощь"
    }
}