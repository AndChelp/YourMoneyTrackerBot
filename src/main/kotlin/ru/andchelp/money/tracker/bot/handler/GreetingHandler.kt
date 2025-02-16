package ru.andchelp.money.tracker.bot.handler

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.andchelp.money.tracker.bot.config.MenuConfig
import ru.andchelp.money.tracker.bot.config.TextKey
import ru.andchelp.money.tracker.bot.handler.type.CallbackHandler
import ru.andchelp.money.tracker.bot.handler.type.CommandHandler
import ru.andchelp.money.tracker.bot.handler.type.ContextualTextMessageHandler
import ru.andchelp.money.tracker.bot.infra.ContextHolder
import ru.andchelp.money.tracker.bot.infra.GreetingNewAccountContext
import ru.andchelp.money.tracker.bot.infra.MsgKeyboard
import ru.andchelp.money.tracker.bot.service.AccountService
import ru.andchelp.money.tracker.bot.service.CategoryService
import ru.andchelp.money.tracker.bot.service.CurrencyService
import ru.andchelp.money.tracker.bot.service.MessageService
import ru.andchelp.money.tracker.bot.service.UserService

@Configuration
class GreetingHandler(
    private val currencyService: CurrencyService,
    private val msgService: MessageService,
    private val userService: UserService,
    private val accountService: AccountService,
    private val categoryService: CategoryService
) {

    @Bean("/start")
    fun startCmd() = CommandHandler { cmd ->
        if (userService.userExists(cmd.userId)) {
            msgService.send(TextKey.USE_MENU_FOR_NAVIGATION, MenuConfig.FULL)
        } else {
            msgService.send(TextKey.FEW_STEPS, MenuConfig.SIMPLE)
            msgService.send(TextKey.SELECT_CURRENCY, currencyService.getKeyboard("global_currency"))
        }
    }

    @Bean("global_currency")
    fun currencyClbk() = CallbackHandler { clbk ->
        msgService.edit(clbk.msgId, "${TextKey.GLOBAL_CURRENCY} ${clbk.data}")
        val user = userService.save(clbk.userId, clbk.data)
        categoryService.addDefaultCategories(user)
        msgService.send(
            TextKey.LAST_STEP_CREATE_NEW_ACC,
            MsgKeyboard().row().button(TextKey.NEW_ACCOUNT, "greeting_new_account")
        )
    }

    @Bean("greeting_new_account")
    fun newAccountClbk() = CallbackHandler { clbk ->
        msgService.edit(clbk.msgId, TextKey.WRITE_ACC_NAME)
        ContextHolder.set(GreetingNewAccountContext(clbk.msgId, "greeting_account_name_msg"))
    }

    @Bean("greeting_account_name_msg")
    fun accountName() = ContextualTextMessageHandler { msg ->
        val context: GreetingNewAccountContext = ContextHolder.current()!!
        context.name = msg.text

        msgService.delete(msg.msgId)
        msgService.edit(
            context.baseMsgId,
            TextKey.ACC_NAME_CHOOSE_CURRENCY.format(msg.text),
            currencyService.getKeyboard("greeting_account_currency").row().button(TextKey.BACK, "greeting_new_account")
        )
    }

    @Bean("greeting_account_currency")
    fun accountCurrencyClbk() = CallbackHandler { clbk ->
        val context: GreetingNewAccountContext = ContextHolder.current()!!
        context.currency = clbk.data

        msgService.edit(
            clbk.msgId,
            TextKey.SCHOOSED_NAME_AND_CURRENCY.format(context.name, clbk.data),
            MsgKeyboard()
                .row().button(TextKey.CONFIRM, "greeting_complete_account_creation")
                .row().button(TextKey.BACK, "greeting_new_account")
        )
    }

    @Bean("greeting_complete_account_creation")
    fun completeAccountCreationClbk() = CallbackHandler { clbk ->
        msgService.delete(clbk.msgId)

        msgService.send(TextKey.END_GREETING_TEXT, MenuConfig.FULL)
        val accountContext: GreetingNewAccountContext = ContextHolder.current()!!
        accountService.newAccount(clbk.userId, accountContext.name!!, accountContext.currency!!)
        ContextHolder.remove()
    }

}