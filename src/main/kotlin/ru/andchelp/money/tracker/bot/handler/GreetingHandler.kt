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
import ru.andchelp.money.tracker.bot.handler.type.CommandHandler
import ru.andchelp.money.tracker.bot.handler.type.TextMessageHandler
import ru.andchelp.money.tracker.bot.infra.ContextHolder
import ru.andchelp.money.tracker.bot.infra.GreetingNewAccountContext
import ru.andchelp.money.tracker.bot.model.User
import ru.andchelp.money.tracker.bot.service.AccountService
import ru.andchelp.money.tracker.bot.service.CurrencyService
import ru.andchelp.money.tracker.bot.service.MessageService
import ru.andchelp.money.tracker.bot.service.UserService

@Configuration
class GreetingHandler(
    val client: TelegramClient,
    val currencyService: CurrencyService,
    val messageService: MessageService,
    val userService: UserService,
    val accountService: AccountService
) {

    @Bean("/start")
    fun startCmd() = CommandHandler { update ->
        val message = update.message

        try {
            userService.findById(message.from.id)
            val msg = SendMessage.builder()
                .chatId(message.chatId)
                .text(
                    "Используйте кнопку меню для навигации"
                )
                .replyMarkup(MenuConfig.FULL)
                .build()
            client.execute(msg)
        } catch (_: Exception){
            client.execute(
                SendMessage.builder()
                    .text(messageService.msgFor("greeting.few.steps"))
                    .chatId(message.chatId)
                    .replyMarkup(MenuConfig.SIMPLE)
                    .build()
            )
            client.execute(
                SendMessage.builder()
                    .chatId(message.chatId)
                    .text(messageService.msgFor("greeting.please.select.currency"))
                    .replyMarkup(currencyService.getCurrenciesKeyboard("global_currency"))
                    .build()
            )
        }
    }

    @Bean("global_currency")
    fun currencyClbk() = CallbackHandler { update ->
        val clbk = update.callbackQuery
        val message = clbk.message
        val editMessageText = EditMessageText.builder()
            .chatId(message.chatId)
            .messageId(message.messageId)
            .text("Основная валюта: ${clbk.data.substringAfter(":")}")
            .build()
        client.execute(editMessageText)

        userService.save(User(clbk.from.id, clbk.data.substringAfter(":")))

        val inlineKeyboardMarkup = InlineKeyboardMarkup(
            listOf(
                InlineKeyboardRow(
                    InlineKeyboardButton.builder()
                        .text("Новый счет")
                        .callbackData("greeting_new_account")
                        .build()
                )
            )
        )
        val build = SendMessage.builder()
            .chatId(message.chatId)
            .text("Еще один шаг - создайте первый счет")
            .replyMarkup(inlineKeyboardMarkup)
            .build()
        client.execute(build)
    }

    @Bean("greeting_new_account")
    fun newAccountClbk() = CallbackHandler { update ->
        val message = update.callbackQuery.message
        val editMessageText = EditMessageText.builder()
            .chatId(message.chatId)
            .messageId(message.messageId)
            .text("Введите название счета")
            .build()
        client.execute(editMessageText)
        ContextHolder.current[message.chatId] = GreetingNewAccountContext(message.messageId)
    }

    @Bean("greeting_account_name_msg")
    fun accountName() = TextMessageHandler { update ->
        val message = update.message

        val context = ContextHolder.current[message.chatId]
        if (context !is GreetingNewAccountContext || context.name != null) return@TextMessageHandler

        val editMessageText = EditMessageText.builder()
            .chatId(message.chatId)
            .messageId(context.baseMsgId)
            .text("Название счета: ${message.text}\nВыберите валюту счета")
            .replyMarkup(
                currencyService
                    .getCurrenciesKeyboard("greeting_account_currency")
                    .addBackButton("greeting_new_account")
            )
            .build()
        client.execute(editMessageText)
        client.execute(DeleteMessage(message.chatId.toString(), message.messageId))

        context.name = message.text
    }

    @Bean("greeting_account_currency")
    fun accountCurrencyClbk() = CallbackHandler { update ->
        val message = update.callbackQuery.message

        val context = ContextHolder.current[message.chatId]
        if (context !is GreetingNewAccountContext || context.currency != null) return@CallbackHandler
        val inlineKeyboardMarkup = InlineKeyboardMarkup(
            mutableListOf(
                InlineKeyboardRow(
                    InlineKeyboardButton.builder()
                        .text("Подтвердить")
                        .callbackData("greeting_complete_account_creation")
                        .build()
                )
            )
        ).addBackButton("greeting_new_account")
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

    @Bean("greeting_complete_account_creation")
    fun completeAccountCreationClbk() = CallbackHandler { update ->
        val message = update.callbackQuery.message

        client.execute(DeleteMessage(message.chatId.toString(), message.messageId))

        val msg = SendMessage.builder()
            .chatId(message.chatId)
            .text(
                "Ваш первый счет создан, теперь можно приступать к работе!\n" +
                        "Можете начать с создания дополнительных счетов, настроить категории под себя или сразу приступить к вводу доходов и расходов, используя соответствующие кнопки меню"
            )
            .replyMarkup(MenuConfig.FULL)
            .build()
        client.execute(msg)
        val accountContext = ContextHolder.current[message.chatId] as GreetingNewAccountContext
        accountService.newAccount(update.callbackQuery.from.id, accountContext.name!!, accountContext.currency!!)
        ContextHolder.current.remove(message.chatId)
    }

}