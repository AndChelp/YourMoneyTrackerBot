package ru.andchelp.money.tracker.bot.handler

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.andchelp.money.tracker.bot.handler.type.CallbackHandler
import ru.andchelp.money.tracker.bot.handler.type.ContextualTextMessageHandler
import ru.andchelp.money.tracker.bot.handler.type.GeneralTextMessageHandler
import ru.andchelp.money.tracker.bot.infra.ContextHolder
import ru.andchelp.money.tracker.bot.infra.MsgKeyboard
import ru.andchelp.money.tracker.bot.infra.NewAccountContext
import ru.andchelp.money.tracker.bot.service.AccountService
import ru.andchelp.money.tracker.bot.service.CurrencyService
import ru.andchelp.money.tracker.bot.service.MessageService
import ru.andchelp.money.tracker.bot.service.UserService

@Configuration
class AccountHandler(
    private val accountService: AccountService,
    private val currencyService: CurrencyService,
    private val userService: UserService,
    private val msgService: MessageService,
) {
    @Bean("account_menu_clicked")
    fun accountMenu() = GeneralTextMessageHandler { msg ->
        if (msg.text != "Счета") return@GeneralTextMessageHandler

        val accountsKeyboard = accountService.getKeyboard(msg.userId)
        if (accountsKeyboard.keyboard.isEmpty()) {
            msgService.send(YOU_DONT_HAVE_ACC_CREATE_NEW, MsgKeyboard().row().button(NEW_ACCOUNT, "new_account"))
        } else {
            val totalBalance = accountService.calcTotalBalance(msg.userId)
            val globalUserCurrency = userService.findById(msg.userId).totalBalanceCurrency
            accountsKeyboard.row().button(NEW_ACCOUNT, "new_account")
            msgService.send(GLOBAL_BALANCE_YOUR_ACCS.format(totalBalance, globalUserCurrency), accountsKeyboard)
        }
    }

    @Bean("new_account")
    fun newAccount() = CallbackHandler { clbk ->
        msgService.edit(clbk.msgId, WRITE_ACC_NAME)
        ContextHolder.current[clbk.chatId] = NewAccountContext(clbk.msgId, "account_name_msg")
    }

    @Bean("account_name_msg")
    fun accountName() = ContextualTextMessageHandler { msg ->
        val context: NewAccountContext = ContextHolder.current()!!

        msgService.edit(
            context.baseMsgId,
            ACC_NAME_CHOOSE_CURRENCY.format(msg.text),
            currencyService.getKeyboard("account_currency").row().button(GreetingHandler.BACK, "new_account")
        )
        msgService.delete(msg.msgId)
        context.name = msg.text
    }

    @Bean("account_currency")
    fun accountCurrencyClbk() = CallbackHandler { clbk ->
        val context: NewAccountContext = ContextHolder.current()!!
        context.currency = clbk.data

        msgService.edit(
            clbk.msgId,
            SCHOOSED_NAME_AND_CURRENCY.format(context.name, clbk.data),
            MsgKeyboard()
                .row().button(GreetingHandler.CONFIRM, "complete_account_creation")
                .row().button(GreetingHandler.BACK, "new_account")
        )
    }

    @Bean("complete_account_creation")
    fun completeAccountCreationClbk() = CallbackHandler { clbk ->
        val accountContext = ContextHolder.current[clbk.chatId] as NewAccountContext

        accountService.newAccount(clbk.userId, accountContext.name!!, accountContext.currency!!)

        val totalBalance = accountService.calcTotalBalance(clbk.userId)
        val userGlobalCurrency = userService.findById(clbk.userId).totalBalanceCurrency

        val accountsKeyboard = accountService.getKeyboard(clbk.userId).row().button(NEW_ACCOUNT, "new_account")

        msgService.edit(clbk.msgId, GLOBAL_BALANCE_YOUR_ACCS.format(totalBalance, userGlobalCurrency), accountsKeyboard)

        ContextHolder.current.remove(clbk.chatId)
    }

    companion object {

        const val YOU_DONT_HAVE_ACC_CREATE_NEW = "У вас еще нет счетов!\nДля создания нового счета нажмите кнопку ниже"
        const val GLOBAL_BALANCE_YOUR_ACCS = "Общий баланс: %s %s\nВаши счета:"

        const val NEW_ACCOUNT = "Новый счет"
        const val WRITE_ACC_NAME = "Введите название счета"

        const val ACC_NAME_CHOOSE_CURRENCY = "Название счета: %s\nВыберите валюту счета"
        const val SCHOOSED_NAME_AND_CURRENCY = "Название счета: %s\nВалюта: %s"
    }
}