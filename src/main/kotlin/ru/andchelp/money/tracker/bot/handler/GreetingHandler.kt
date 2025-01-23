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
    fun startCmd() = CommandHandler { cmd ->

        try {
            userService.findById(cmd.userId)
            val msg = SendMessage.builder()
                .chatId(cmd.chatId)
                .text(
                    "Используйте кнопку меню для навигации"
                )
                .replyMarkup(MenuConfig.FULL)
                .build()
            client.execute(msg)
        } catch (_: Exception) {
            client.execute(
                SendMessage.builder()
                    .text(messageService.msgFor("greeting.few.steps"))
                    .chatId(cmd.chatId)
                    .replyMarkup(MenuConfig.SIMPLE)
                    .build()
            )
            client.execute(
                SendMessage.builder()
                    .chatId(cmd.chatId)
                    .text(messageService.msgFor("greeting.please.select.currency"))
                    .replyMarkup(currencyService.getCurrenciesKeyboard("global_currency"))
                    .build()
            )
        }
    }

    @Bean("global_currency")
    fun currencyClbk() = CallbackHandler { clbk ->
        val editMessageText = EditMessageText.builder()
            .chatId(clbk.chatId)
            .messageId(clbk.msgId)
            .text("Основная валюта: ${clbk.data}")
            .build()
        client.execute(editMessageText)

        userService.save(User(clbk.userId, clbk.data))

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
            .chatId(clbk.chatId)
            .text("Еще один шаг - создайте первый счет")
            .replyMarkup(inlineKeyboardMarkup)
            .build()
        client.execute(build)
    }

    @Bean("greeting_new_account")
    fun newAccountClbk() = CallbackHandler { clbk ->
        val editMessageText = EditMessageText.builder()
            .chatId(clbk.chatId)
            .messageId(clbk.msgId)
            .text("Введите название счета")
            .build()
        client.execute(editMessageText)
        ContextHolder.current[clbk.chatId] = GreetingNewAccountContext(clbk.msgId)
    }

    @Bean("greeting_account_name_msg")
    fun accountName() = TextMessageHandler { msg ->

        val context = ContextHolder.current[msg.chatId]
        if (context !is GreetingNewAccountContext || context.name != null) return@TextMessageHandler

        val editMessageText = EditMessageText.builder()
            .chatId(msg.chatId)
            .messageId(context.baseMsgId)
            .text("Название счета: ${msg.text}\nВыберите валюту счета")
            .replyMarkup(
                currencyService
                    .getCurrenciesKeyboard("greeting_account_currency")
                    .addBackButton("greeting_new_account")
            )
            .build()
        client.execute(editMessageText)
        client.execute(DeleteMessage(msg.chatId.toString(), msg.msgId))

        context.name = msg.text
    }

    @Bean("greeting_account_currency")
    fun accountCurrencyClbk() = CallbackHandler { clbk ->

        val context = ContextHolder.current[clbk.chatId]
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
        val editMessageText = EditMessageText.builder()
            .chatId(clbk.chatId)
            .messageId(clbk.msgId)
            .text(
                "Название счета: ${context.name}\n" +
                        "Валюта: ${clbk.data}"
            )
            .replyMarkup(inlineKeyboardMarkup)
            .build()
        client.execute(editMessageText)

        context.currency = clbk.data
    }

    @Bean("greeting_complete_account_creation")
    fun completeAccountCreationClbk() = CallbackHandler { clbk ->

        client.execute(DeleteMessage(clbk.chatId.toString(), clbk.msgId))

        val msg = SendMessage.builder()
            .chatId(clbk.chatId)
            .text(
                "Ваш первый счет создан, теперь можно приступать к работе!\n" +
                        "Можете начать с создания дополнительных счетов, настроить категории под себя или сразу приступить к вводу доходов и расходов, используя соответствующие кнопки меню"
            )
            .replyMarkup(MenuConfig.FULL)
            .build()
        client.execute(msg)
        val accountContext = ContextHolder.current[clbk.chatId] as GreetingNewAccountContext
        accountService.newAccount(clbk.userId, accountContext.name!!, accountContext.currency!!)
        ContextHolder.current.remove(clbk.chatId)
    }

}