package ru.andchelp.money.tracker.bot.handler

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow
import ru.andchelp.money.tracker.bot.handler.type.CallbackHandler
import ru.andchelp.money.tracker.bot.handler.type.TextMessageHandler
import ru.andchelp.money.tracker.bot.id
import ru.andchelp.money.tracker.bot.infra.ContextHolder
import ru.andchelp.money.tracker.bot.infra.NewAccountContext
import ru.andchelp.money.tracker.bot.service.AccountService
import ru.andchelp.money.tracker.bot.service.CurrencyService
import ru.andchelp.money.tracker.bot.service.MessageService
import ru.andchelp.money.tracker.bot.service.UserService
import ru.andchelp.money.tracker.bot.withRow

@Configuration
class AccountHandler(
    private val accountService: AccountService,
    private val currencyService: CurrencyService,
    private val userService: UserService,
    private val msgService: MessageService,
) {
    @Bean("account_menu_clicked")
    fun accountMenu() = TextMessageHandler { msg ->
        if (msg.text != "Счета" || ContextHolder.current[msg.chatId] != null) return@TextMessageHandler

        val newAccKeyboard = InlineKeyboardButton(NEW_ACCOUNT).id("new_account")
        val accountsKeyboard = accountService.getKeyboard(msg.userId)
        if (accountsKeyboard.isEmpty()) {
            msgService.send(YOU_DONT_HAVE_ACC_CREATE_NEW, newAccKeyboard)
        } else {
            val totalBalance = accountService.calcTotalBalance(msg.userId)
            val globalUserCurrency = userService.findById(msg.userId).totalBalanceCurrency
            accountsKeyboard.withRow(newAccKeyboard)
            msgService.send(GLOBAL_BALANCE_YOUR_ACCS.format(totalBalance, globalUserCurrency), accountsKeyboard)
        }
    }

    @Bean("new_account")
    fun newAccount() = CallbackHandler { clbk ->
        msgService.edit(clbk.msgId, WRITE_ACC_NAME)
        ContextHolder.current[clbk.chatId] = NewAccountContext(clbk.msgId)
    }

    @Bean("account_name_msg")
    fun accountName() = TextMessageHandler { msg ->
        val context: NewAccountContext? = ContextHolder.current()
        if (!(context != null && context.name == null)) return@TextMessageHandler

        msgService.edit(
            context.baseMsgId,
            ACC_NAME_CHOOSE_CURRENCY.format(msg.text),
            currencyService
                .getKeyboard("account_currency")
                .withRow(InlineKeyboardButton(GreetingHandler.BACK).id("new_account"))
        )
        msgService.delete(msg.msgId)
        context.name = msg.text
    }

    @Bean("account_currency")
    fun accountCurrencyClbk() = CallbackHandler { clbk ->
        val context: NewAccountContext? = ContextHolder.current()
        if (!(context != null && context.currency == null)) return@CallbackHandler
        context.currency = clbk.data

        msgService.edit(
            clbk.msgId,
            SCHOOSED_NAME_AND_CURRENCY.format(context.name, clbk.data),
            mutableListOf(
                InlineKeyboardRow(InlineKeyboardButton(GreetingHandler.CONFIRM).id("complete_account_creation"))
            ).withRow(InlineKeyboardButton(GreetingHandler.BACK).id("new_account"))
        )
    }

    @Bean("complete_account_creation")
    fun completeAccountCreationClbk() = CallbackHandler { clbk ->
        val accountContext = ContextHolder.current[clbk.chatId] as NewAccountContext

        accountService.newAccount(clbk.userId, accountContext.name!!, accountContext.currency!!)

        val totalBalance = accountService.calcTotalBalance(clbk.userId)
        val userGlobalCurrency = userService.findById(clbk.userId).totalBalanceCurrency

        val accountsKeyboard = accountService.getKeyboard(clbk.userId)
            .withRow(InlineKeyboardButton(GreetingHandler.NEW_ACCOUNT).id("new_account"))

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