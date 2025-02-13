package ru.andchelp.money.tracker.bot.handler

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.andchelp.money.tracker.bot.config.TextKey
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
        if (msg.text != TextKey.ACCOUNTS) return@GeneralTextMessageHandler
        val accountsKeyboard = accountService.getKeyboard(msg.userId, "account_info")

        val text = if (accountsKeyboard.keyboard.isEmpty()) {
            TextKey.YOU_DONT_HAVE_ACC_CREATE_NEW
        } else {
            val totalBalance = accountService.calcTotalBalance(msg.userId)
            val globalUserCurrency = userService.findById(msg.userId).globalCurrency!!.symbol
            TextKey.GLOBAL_BALANCE_YOUR_ACCS.format(totalBalance, globalUserCurrency)
        }
        msgService.send(text, accountsKeyboard.row().button(TextKey.NEW_ACCOUNT, "new_account"))
    }

    @Bean("account_list_clbk")
    fun accountListClbk() = CallbackHandler { clbk ->
        val accountsKeyboard = accountService.getKeyboard(clbk.userId, "account_info")
        if (accountsKeyboard.keyboard.isEmpty()) {
            msgService.edit(
                clbk.msgId,
                TextKey.YOU_DONT_HAVE_ACC_CREATE_NEW,
                MsgKeyboard().row().button(TextKey.NEW_ACCOUNT, "new_account")
            )
        } else {
            val totalBalance = accountService.calcTotalBalance(clbk.userId)
            val globalUserCurrency = userService.findById(clbk.userId).globalCurrency!!.symbol
            accountsKeyboard.row().button(TextKey.NEW_ACCOUNT, "new_account")
            msgService.edit(
                clbk.msgId,
                TextKey.GLOBAL_BALANCE_YOUR_ACCS.format(totalBalance, globalUserCurrency),
                accountsKeyboard
            )
        }
    }

    @Bean("new_account")
    fun newAccount() = CallbackHandler { clbk ->
        msgService.edit(clbk.msgId, TextKey.WRITE_ACC_NAME)
        ContextHolder.set(NewAccountContext(clbk.msgId, "account_name_msg"))
    }

    @Bean("account_name_msg")
    fun accountName() = ContextualTextMessageHandler { msg ->
        val context: NewAccountContext = ContextHolder.current()!!

        msgService.edit(
            context.baseMsgId,
            TextKey.ACC_NAME_CHOOSE_CURRENCY.format(msg.text),
            currencyService.getKeyboard("account_currency").row().button(TextKey.BACK, "new_account")
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
            TextKey.SCHOOSED_NAME_AND_CURRENCY.format(context.name, clbk.data),
            MsgKeyboard()
                .row().button(TextKey.CONFIRM, "complete_account_creation")
                .row().button(TextKey.BACK, "new_account")
        )
    }

    @Bean("complete_account_creation")
    fun completeAccountCreationClbk() = CallbackHandler { clbk ->
        val accountContext: NewAccountContext = ContextHolder.current()!!

        accountService.newAccount(clbk.userId, accountContext.name!!, accountContext.currency!!)

        val totalBalance = accountService.calcTotalBalance(clbk.userId)
        val userGlobalCurrency = userService.findById(clbk.userId).globalCurrency!!.symbol

        val accountsKeyboard = accountService.getKeyboard(clbk.userId, "account_info")
            .row().button(TextKey.NEW_ACCOUNT, "new_account")

        msgService.edit(
            clbk.msgId,
            TextKey.GLOBAL_BALANCE_YOUR_ACCS.format(totalBalance, userGlobalCurrency),
            accountsKeyboard
        )

        ContextHolder.remove()
    }

    @Bean("account_info")
    fun accountInfo() = CallbackHandler { clbk ->
        ContextHolder.remove()

        val account = accountService.findById(clbk.data.toLong())
        msgService.edit(
            clbk.msgId, "Счет \"${account.name}\"\n" +
                    "🆔 Идентификатор: ${account.id}\n" +
                    "🏦 Баланс: ${account.balance}${account.currency!!.symbol}\n" +
                    "💱 Валюта: ${msgService.msgFor(account.currency!!.name!!)}\n" +
                    "⚖️ Учитывать в общем балансе: ${if (account.allowInTotalBalance) "✓" else "✗"}\n" +
                    "📆 Дата создания: ${account.creationDate.toLocalDate()}",
            MsgKeyboard()
                .row().button(TextKey.EDIT, "change_account_info", account.id)
                .row().button(TextKey.BACK, "account_list_clbk")
        )
    }

    @Bean("change_account_info")
    fun changeAccountInfo() = CallbackHandler { clbk ->
        val account = accountService.findById(clbk.data.toLong())
        changeAccountInfo(account, clbk.msgId)
    }

    private fun changeAccountInfo(account: Account, msgId: Int) {
        ContextHolder.remove()
        msgService.edit(
            msgId,
            "Изменение параметров счета",
            MsgKeyboard()
                .row().button("🔤 Название: ${account.name}", "change_account_name", account.id)
                .row().button(
                    "💱 Валюта: ${msgService.msgFor(account.currency!!.name!!)}",
                    "change_account_currency",
                    account.id
                )
                .row().button(
                    "⚖️ Учитывать в общем балансе: ${if (account.allowInTotalBalance) "✓" else "✗"}\n",
                    "change_allow_in_total_balance", account.id
                )
                .row().button(TextKey.DELETE, "delete_account", account.id)
                .row().button(TextKey.BACK, "account_info", account.id)
        )
    }

    @Bean("change_account_name")
    fun changeAccountName() = CallbackHandler { clbk ->
        ContextHolder.set(
            EditAccountContext(clbk.msgId, "change_account_name_input", accountId = clbk.data.toLong())
        )

        msgService.edit(
            clbk.msgId,
            "Укажите новое название",
            MsgKeyboard().row().button(TextKey.BACK, "change_account_info", clbk.data)
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
        ContextHolder.set(EditAccountContext(clbk.msgId, accountId = clbk.data.toLong()))

        msgService.edit(
            clbk.msgId,
            "Выберите новую валюту счета.\nВсе связанные операции будут перечитаны по состоянию курса на момент записи!",
            currencyService.getKeyboard("change_account_currency_input").row()
                .button(TextKey.BACK, "change_account_info", clbk.data)
        )
    }

    @Bean("change_account_currency_input")
    fun changeAccountCurrencyInput() = CallbackHandler { clbk ->
        val context: EditAccountContext = ContextHolder.current()!!
        val account = accountService.findById(context.accountId!!)
        account.currency = currencyService.findByCode(clbk.data)
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
            MsgKeyboard().row().button(TextKey.CONFIRM, "confirm_account_deletion", clbk.data)
                .button(TextKey.BACK, "change_account_info", clbk.data)
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

}