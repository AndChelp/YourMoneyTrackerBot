package ru.andchelp.money.tracker.bot.handler

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.andchelp.money.tracker.bot.handler.type.CallbackHandler
import ru.andchelp.money.tracker.bot.handler.type.ContextualTextMessageHandler
import ru.andchelp.money.tracker.bot.handler.type.GeneralTextMessageHandler
import ru.andchelp.money.tracker.bot.infra.ContextHolder
import ru.andchelp.money.tracker.bot.infra.EditAccountContext
import ru.andchelp.money.tracker.bot.infra.MsgKeyboard
import ru.andchelp.money.tracker.bot.infra.NewAccountContext
import ru.andchelp.money.tracker.bot.model.Account
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
    @Bean("account_list")
    fun accountList() = GeneralTextMessageHandler { msg ->
        if (msg.text != "Счета") return@GeneralTextMessageHandler
        val accountsKeyboard = accountService.getKeyboard(msg.userId, "account_info")
        if (accountsKeyboard.keyboard.isEmpty()) {
            msgService.send(YOU_DONT_HAVE_ACC_CREATE_NEW, MsgKeyboard().row().button(NEW_ACCOUNT, "new_account"))
        } else {
            val totalBalance = accountService.calcTotalBalance(msg.userId)
            val globalUserCurrency = userService.findById(msg.userId).totalBalanceCurrency
            accountsKeyboard.row().button(NEW_ACCOUNT, "new_account")
            msgService.send(GLOBAL_BALANCE_YOUR_ACCS.format(totalBalance, globalUserCurrency), accountsKeyboard)
        }
    }

    @Bean("account_list_clbk")
    fun accountListClbk() = CallbackHandler { clbk ->
        val accountsKeyboard = accountService.getKeyboard(clbk.userId, "account_info")
        if (accountsKeyboard.keyboard.isEmpty()) {
            msgService.edit(
                clbk.msgId,
                YOU_DONT_HAVE_ACC_CREATE_NEW,
                MsgKeyboard().row().button(NEW_ACCOUNT, "new_account")
            )
        } else {
            val totalBalance = accountService.calcTotalBalance(clbk.userId)
            val globalUserCurrency = userService.findById(clbk.userId).totalBalanceCurrency
            accountsKeyboard.row().button(NEW_ACCOUNT, "new_account")
            msgService.edit(
                clbk.msgId,
                GLOBAL_BALANCE_YOUR_ACCS.format(totalBalance, globalUserCurrency),
                accountsKeyboard
            )
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
        val accountContext: NewAccountContext = ContextHolder.current()!!

        accountService.newAccount(clbk.userId, accountContext.name!!, accountContext.currency!!)

        val totalBalance = accountService.calcTotalBalance(clbk.userId)
        val userGlobalCurrency = userService.findById(clbk.userId).totalBalanceCurrency

        val accountsKeyboard = accountService.getKeyboard(clbk.userId, "account_info")
            .row().button(NEW_ACCOUNT, "new_account")

        msgService.edit(clbk.msgId, GLOBAL_BALANCE_YOUR_ACCS.format(totalBalance, userGlobalCurrency), accountsKeyboard)

        ContextHolder.current.remove(clbk.chatId)
    }

    @Bean("account_info")
    fun accountInfo() = CallbackHandler { clbk ->
        ContextHolder.removeContext()

        val account = accountService.findById(clbk.data.toLong())
        msgService.edit(
            clbk.msgId, "Счет ${account.name}\n" +
                    "Баланс: ${account.balance}\n" +
                    "Валюта: ${account.currencyCode}\n" +
                    "Учитывать в общем балансе: ${if (account.allowInTotalBalance) "+" else "-"}\n" +
                    "Дата создания: ${account.creationDate.toLocalDate()}",
            MsgKeyboard()
                .row().button("Редактировать", "change_account_info", account.id)
                .row().button("<< Назад", "account_list_clbk")
        )
    }

    @Bean("change_account_info")
    fun changeAccountInfo() = CallbackHandler { clbk ->
        val account = accountService.findById(clbk.data.toLong())
        changeAccountInfo(account, clbk.msgId)
    }

    private fun changeAccountInfo(account: Account, msgId: Int) {
        ContextHolder.removeContext()
        msgService.edit(
            msgId,
            "Изменение параметров счета",
            MsgKeyboard()
                .row().button("Название: ${account.name}", "change_account_name", account.id)
                .row().button("Валюта: ${account.currencyCode}", "change_account_currency", account.id)
                .row().button(
                    "Учитывать в общем балансе: ${if (account.allowInTotalBalance) "+" else "-"}\n",
                    "change_allow_in_total_balance", account.id
                )
                .row().button("Удалить счет", "delete_account", account.id)
                .row().button("<< Назад", "account_info", account.id)
        )
    }

    @Bean("change_account_name")
    fun changeAccountName() = CallbackHandler { clbk ->
        ContextHolder.current[clbk.chatId] =
            EditAccountContext(clbk.msgId, "change_account_name_input", accountId = clbk.data.toLong())

        msgService.edit(
            clbk.msgId,
            "Укажите новое название",
            MsgKeyboard().row().button("<< Назад", "change_account_info", clbk.data)
        )
    }

    @Bean("change_account_name_input")
    fun changeAccountNameInput() = ContextualTextMessageHandler { msg ->
        val context: EditAccountContext = ContextHolder.current()!!
        val account = accountService.findById(context.accountId!!)
        account.name = msg.text
        accountService.save(account)

        changeAccountInfo(account, context.baseMsgId)
        msgService.delete(msg.msgId)
    }

    @Bean("change_account_currency")
    fun changeAccountCurrency() = CallbackHandler { clbk ->
        ContextHolder.current[clbk.chatId] = EditAccountContext(clbk.msgId, accountId = clbk.data.toLong())

        msgService.edit(
            clbk.msgId,
            "Выберите новую валюту счета.\nВсе связанные операции будут перечитаны по состоянию курса на момент записи!",
            currencyService.getKeyboard("change_account_currency_input").row()
                .button(GreetingHandler.BACK, "change_account_info", clbk.data)
        )
    }

    @Bean("change_account_currency_input")
    fun changeAccountCurrencyInput() = CallbackHandler { clbk ->
        val context: EditAccountContext = ContextHolder.current()!!
        val account = accountService.findById(context.accountId!!)
        account.currencyCode = clbk.data
        accountService.save(account)

        //todo пересчет валют

        changeAccountInfo(account, clbk.msgId)
    }

    @Bean("change_allow_in_total_balance")
    fun changeAllowInTotalBalance() = CallbackHandler { clbk ->
        val account = accountService.findById(clbk.data.toLong())
        account.allowInTotalBalance = !account.allowInTotalBalance
        accountService.save(account)

        changeAccountInfo(account, clbk.msgId)
    }


    @Bean("delete_account")
    fun deleteAccount() = CallbackHandler { clbk ->
        msgService.edit(
            clbk.msgId, "Вы уверены что хотите удалить счет?\nВсе связанные операции также будут удалены!",
            MsgKeyboard().row().button("Подтвердить", "confirm_account_deletion", clbk.data)
                .button(GreetingHandler.BACK, "change_account_info", clbk.data)
        )
    }


    @Bean("confirm_account_deletion")
    fun confirmAccountDeletion() = CallbackHandler { clbk ->
        accountService.delete(clbk.data.toLong())
        msgService.edit(
            clbk.msgId, "Счет и все связанные операции были удалены!",
            MsgKeyboard().row().button("Вернуться к счетам", "account_list_clbk")
        )
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