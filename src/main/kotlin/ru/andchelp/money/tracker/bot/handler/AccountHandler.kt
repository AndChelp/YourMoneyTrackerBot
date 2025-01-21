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
import ru.andchelp.money.tracker.bot.config.MenuConfig
import ru.andchelp.money.tracker.bot.handler.type.CallbackHandler
import ru.andchelp.money.tracker.bot.handler.type.TextMessageHandler
import ru.andchelp.money.tracker.bot.infra.ContextHolder
import ru.andchelp.money.tracker.bot.infra.GreetingNewAccountContext
import ru.andchelp.money.tracker.bot.infra.NewAccountContext
import ru.andchelp.money.tracker.bot.service.AccountService
import ru.andchelp.money.tracker.bot.service.CurrencyService

@Configuration
class AccountHandler(
    val client: TelegramClient,
    val accountService: AccountService,
    val currencyService: CurrencyService
) {
    @Bean("account_menu_clicked")
    fun accountMenu() = TextMessageHandler { update ->
        val message = update.message
        if (message.text != "Счета" || ContextHolder.current[message.chatId] != null) return@TextMessageHandler
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

        val accounts = accountService.findAccounts(message.from.id)
        val sendMessage = if (accounts.isEmpty()) {
            SendMessage.builder()
                .text(
                    "У вас еще нет счетов!\n" +
                            "Для создания нового счета нажмите кнопку ниже"
                )
                .chatId(message.chatId)
                .replyMarkup(inlineKeyboardMarkup)
                .build()
        } else {
            val totalBalance = accountService.calcTotalBalance(message.from.id)
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

            SendMessage.builder()
                .text(
                    "Общий баланс: $totalBalance ₽\n" +
                            "Ваши счета:"
                )
                .chatId(message.chatId)
                .replyMarkup(let)
                .build()
        }

        client.execute(
            sendMessage
        )
    }

    @Bean("new_account")
    fun newAccount() = CallbackHandler { update ->
        val message = update.callbackQuery.message
        val editMessageText = EditMessageText.builder()
            .chatId(message.chatId)
            .messageId(message.messageId)
            .text("Введите название счета")
            .build()
        client.execute(editMessageText)
        ContextHolder.current[message.chatId] = NewAccountContext(message.messageId)
    }

    @Bean("account_name_msg")
    fun accountName() = TextMessageHandler { update ->
        val message = update.message

        val context = ContextHolder.current[message.chatId]
        if (context !is NewAccountContext || context.name != null) return@TextMessageHandler

        val editMessageText = EditMessageText.builder()
            .chatId(message.chatId)
            .messageId(context.baseMsgId)
            .text("Название счета: ${message.text}\nВыберите валюту счета")
            .replyMarkup(
                currencyService
                    .getCurrenciesKeyboard("account_currency")
                    .addBackButton("new_account")
            )
            .build()
        client.execute(editMessageText)
        client.execute(DeleteMessage(message.chatId.toString(), message.messageId))

        context.name = message.text
    }

    @Bean("account_currency")
    fun accountCurrencyClbk() = CallbackHandler { update ->
        val message = update.callbackQuery.message

        val context = ContextHolder.current[message.chatId]
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
        val currency = update.callbackQuery.data.substringAfter(":")
        val editMessageText = EditMessageText.builder()
            .chatId(message.chatId)
            .messageId(message.messageId)
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
    fun completeAccountCreationClbk() = CallbackHandler { update ->
        val message = update.callbackQuery.message
        val accountContext = ContextHolder.current[message.chatId] as NewAccountContext

        accountService.newAccount(update.callbackQuery.from.id, accountContext.name!!, accountContext.currency!!)

        val accounts = accountService.findAccounts(update.callbackQuery.from.id)
            val totalBalance = accountService.calcTotalBalance(update.callbackQuery.from.id)
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

        val sendMessage = EditMessageText.builder()
            .text(
                "Общий баланс: $totalBalance ₽\n" +
                        "Ваши счета:"
            )
            .chatId(message.chatId)
            .messageId(message.messageId)
            .replyMarkup(let)
            .build()

        client.execute(
            sendMessage
        )
        ContextHolder.current.remove(message.chatId)
    }
}