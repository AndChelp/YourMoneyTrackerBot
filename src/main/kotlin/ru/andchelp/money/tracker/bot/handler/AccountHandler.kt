package ru.andchelp.money.tracker.bot.handler

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.andchelp.money.tracker.bot.addBackButton
import ru.andchelp.money.tracker.bot.handler.type.CallbackHandler
import ru.andchelp.money.tracker.bot.handler.type.TextMessageHandler
import ru.andchelp.money.tracker.bot.infra.ContextHolder
import ru.andchelp.money.tracker.bot.infra.NewAccountContext
import ru.andchelp.money.tracker.bot.service.AccountService
import ru.andchelp.money.tracker.bot.service.CurrencyService
import ru.andchelp.money.tracker.bot.service.UserService

@Configuration
class AccountHandler(
    val client: TelegramClient,
    val accountService: AccountService,
    val currencyService: CurrencyService,
    val userService: UserService
) {
    @Bean("account_menu_clicked")
    fun accountMenu() = TextMessageHandler { msg ->
        if (msg.text != "Счета" || ContextHolder.current[msg.chatId] != null) return@TextMessageHandler
        val inlineKeyboardMarkup = InlineKeyboardMarkup(
            listOf(
                InlineKeyboardRow(
                    InlineKeyboardButton.builder()
                        .text("Новый счет")
                        .callbackData("new_account")
                        .build()
                )
            )
        )

        val accounts = accountService.findAccounts(msg.userId)
        val sendMessage = if (accounts.isEmpty()) {
            SendMessage.builder()
                .text(
                    "У вас еще нет счетов!\n" +
                            "Для создания нового счета нажмите кнопку ниже"
                )
                .chatId(msg.chatId)
                .replyMarkup(inlineKeyboardMarkup)
                .build()
        } else {
            val totalBalance = accountService.calcTotalBalance(msg.userId)
            val let = accounts.map {
                InlineKeyboardRow(
                    InlineKeyboardButton.builder()
                        .text("${it.name} - ${it.balance!!.balance} ${it.currencyCode}")
                        .callbackData("account_clbk:${it.id}")
                        .build()
                )
            }.let { InlineKeyboardMarkup(it) }
            let.keyboard.add(
                InlineKeyboardRow(
                    InlineKeyboardButton.builder()
                        .text("Новый счет")
                        .callbackData("new_account")
                        .build()
                )
            )
            val user = userService.findById(msg.userId)

            SendMessage.builder()
                .text(
                    "Общий баланс: $totalBalance ${user.totalBalanceCurrency}\n" +
                            "Ваши счета:"
                )
                .chatId(msg.chatId)
                .replyMarkup(let)
                .build()
        }

        client.execute(
            sendMessage
        )
    }

    @Bean("new_account")
    fun newAccount() = CallbackHandler { clbk ->
        val editMessageText = EditMessageText.builder()
            .chatId(clbk.chatId)
            .messageId(clbk.msgId)
            .text("Введите название счета")
            .build()
        client.execute(editMessageText)
        ContextHolder.current[clbk.chatId] = NewAccountContext(clbk.msgId)
    }

    @Bean("account_name_msg")
    fun accountName() = TextMessageHandler { msg ->
        val context = ContextHolder.current[msg.chatId]
        if (context !is NewAccountContext || context.name != null) return@TextMessageHandler

        val editMessageText = EditMessageText.builder()
            .chatId(msg.chatId)
            .messageId(context.baseMsgId)
            .text("Название счета: ${msg.text}\nВыберите валюту счета")
            .replyMarkup(
                currencyService
                    .getCurrenciesKeyboard("account_currency")
                    .addBackButton("new_account")
            )
            .build()
        client.execute(editMessageText)
        client.execute(DeleteMessage(msg.chatId.toString(), msg.msgId))

        context.name = msg.text
    }

    @Bean("account_currency")
    fun accountCurrencyClbk() = CallbackHandler { clbk ->
        val context = ContextHolder.current[clbk.chatId]
        if (context !is NewAccountContext || context.currency != null) return@CallbackHandler
        val inlineKeyboardMarkup = InlineKeyboardMarkup(
            mutableListOf(
                InlineKeyboardRow(
                    InlineKeyboardButton.builder()
                        .text("Подтвердить")
                        .callbackData("complete_account_creation")
                        .build()
                )
            )
        ).addBackButton("new_account")
        val currency = clbk.data
        val editMessageText = EditMessageText.builder()
            .chatId(clbk.chatId)
            .messageId(clbk.msgId)
            .text(
                "Название счета: ${context.name}\n" +
                        "Валюта: $currency"
            )
            .replyMarkup(inlineKeyboardMarkup)
            .build()
        client.execute(editMessageText)

        context.currency = currency
    }

    @Bean("complete_account_creation")
    fun completeAccountCreationClbk() = CallbackHandler { clbk ->
        val accountContext = ContextHolder.current[clbk.chatId] as NewAccountContext

        accountService.newAccount(clbk.userId, accountContext.name!!, accountContext.currency!!)

        val accounts = accountService.findAccounts(clbk.userId)
        val totalBalance = accountService.calcTotalBalance(clbk.userId)
        val let = accounts.map {
            InlineKeyboardRow(
                InlineKeyboardButton.builder()
                    .text("${it.name} - ${it.balance!!.balance} ${it.currencyCode}")
                    .callbackData("account_clbk:${it.id}")
                    .build()
            )
        }.let { InlineKeyboardMarkup(it) }
        let.keyboard.add(
            InlineKeyboardRow(
                InlineKeyboardButton.builder()
                    .text("Новый счет")
                    .callbackData("new_account")
                    .build()
            )
        )

        val user = userService.findById(clbk.userId)

        val sendMessage = EditMessageText.builder()
            .text(
                "Общий баланс: $totalBalance ${user.totalBalanceCurrency}\n" +
                        "Ваши счета:"
            )
            .chatId(clbk.chatId)
            .messageId(clbk.msgId)
            .replyMarkup(let)
            .build()

        client.execute(
            sendMessage
        )
        ContextHolder.current.remove(clbk.chatId)
    }
}