package ru.andchelp.money.tracker.bot.handler

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow
import ru.andchelp.money.tracker.bot.config.MenuConfig
import ru.andchelp.money.tracker.bot.handler.type.CallbackHandler
import ru.andchelp.money.tracker.bot.handler.type.CommandHandler
import ru.andchelp.money.tracker.bot.handler.type.TextMessageHandler
import ru.andchelp.money.tracker.bot.id
import ru.andchelp.money.tracker.bot.infra.ContextHolder
import ru.andchelp.money.tracker.bot.infra.GreetingNewAccountContext
import ru.andchelp.money.tracker.bot.model.User
import ru.andchelp.money.tracker.bot.service.AccountService
import ru.andchelp.money.tracker.bot.service.CurrencyService
import ru.andchelp.money.tracker.bot.service.MessageService
import ru.andchelp.money.tracker.bot.service.UserService
import ru.andchelp.money.tracker.bot.withRow

@Configuration
class GreetingHandler(
    private val currencyService: CurrencyService,
    private val msgService: MessageService,
    private val userService: UserService,
    private val accountService: AccountService
) {

    @Bean("/start")
    fun startCmd() = CommandHandler { cmd ->
        if (userService.userExists(cmd.userId)) {
            msgService.send(USE_MENU_FOR_NAVIGATION, *MenuConfig.FULL.toTypedArray())
        } else {
            msgService.send(FEW_STEPS, *MenuConfig.SIMPLE.toTypedArray())
            msgService.send(SELECT_CURRENCY, currencyService.getKeyboard("global_currency"))
        }
    }

    @Bean("global_currency")
    fun currencyClbk() = CallbackHandler { clbk ->
        msgService.edit(clbk.msgId, "$GLOBAL_CURRENCY ${clbk.data}")
        userService.save(User(clbk.userId, clbk.data))
        msgService.send(LAST_STEP_CREATE_NEW_ACC, InlineKeyboardButton(NEW_ACCOUNT).id("greeting_new_account"))
    }

    @Bean("greeting_new_account")
    fun newAccountClbk() = CallbackHandler { clbk ->
        msgService.edit(clbk.msgId, WRITE_ACC_NAME)
        ContextHolder.current[clbk.chatId] = GreetingNewAccountContext(clbk.msgId)
    }

    @Bean("greeting_account_name_msg")
    fun accountName() = TextMessageHandler { msg ->
        val context: GreetingNewAccountContext? = ContextHolder.current()
        if (context == null || context.name != null) return@TextMessageHandler
        context.name = msg.text

        msgService.delete(msg.msgId)
        msgService.edit(
            context.baseMsgId,
            ACC_NAME_CHOOSE_CURRENCY.format(msg.text),
            currencyService
                .getKeyboard("greeting_account_currency")
                .withRow(InlineKeyboardButton(BACK).id("greeting_new_account"))
        )
    }

    @Bean("greeting_account_currency")
    fun accountCurrencyClbk() = CallbackHandler { clbk ->
        val context: GreetingNewAccountContext? = ContextHolder.current()
        if (context == null || context.currency != null) return@CallbackHandler
        context.currency = clbk.data

        msgService.edit(
            clbk.msgId,
            SCHOOSED_NAME_AND_CURRENCY.format(context.name, clbk.data),
            mutableListOf(
                InlineKeyboardRow(InlineKeyboardButton(CONFIRM).id("greeting_complete_account_creation"))
            ).withRow(InlineKeyboardButton(BACK).id("greeting_new_account"))
        )
    }

    @Bean("greeting_complete_account_creation")
    fun completeAccountCreationClbk() = CallbackHandler { clbk ->
        msgService.delete(clbk.msgId)

        msgService.send(END_GREETING_TEXT, *MenuConfig.FULL.toTypedArray())
        val accountContext: GreetingNewAccountContext = ContextHolder.current()!!
        accountService.newAccount(clbk.userId, accountContext.name!!, accountContext.currency!!)
        ContextHolder.current.remove(clbk.chatId)
    }

    companion object {
        const val USE_MENU_FOR_NAVIGATION = "Используйте кнопки меню для навигации"
        const val SELECT_CURRENCY = "greeting.please.select.currency"
        const val FEW_STEPS = "greeting.few.steps"
        const val LAST_STEP_CREATE_NEW_ACC = "Еще один шаг - создайте первый счет"
        const val GLOBAL_CURRENCY = "Основная валюта:"
        const val NEW_ACCOUNT = "Новый счет"
        const val BACK = "Назад"
        const val CONFIRM = "Подтвердить"
        const val WRITE_ACC_NAME = "Введите название счета"
        const val ACC_NAME_CHOOSE_CURRENCY = "Название счета: %s\nВыберите валюту счета"
        const val SCHOOSED_NAME_AND_CURRENCY = "Название счета: %s\nВалюта: %s"
        const val END_GREETING_TEXT = "Ваш первый счет создан, теперь можно приступать к работе!\n" +
                "Можете начать с создания дополнительных счетов, настроить категории под себя или сразу приступить к " +
                "вводу доходов и расходов, используя соответствующие кнопки меню"
    }

}