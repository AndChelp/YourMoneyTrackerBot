package ru.andchelp.money.tracker.bot.handler

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.andchelp.money.tracker.bot.config.TextKey
import ru.andchelp.money.tracker.bot.handler.type.CallbackHandler
import ru.andchelp.money.tracker.bot.handler.type.CallbackUpdate
import ru.andchelp.money.tracker.bot.handler.type.ContextualTextMessageHandler
import ru.andchelp.money.tracker.bot.handler.type.GeneralTextMessageHandler
import ru.andchelp.money.tracker.bot.infra.ContextHolder
import ru.andchelp.money.tracker.bot.infra.MsgKeyboard
import ru.andchelp.money.tracker.bot.infra.ShoppingListItemContext
import ru.andchelp.money.tracker.bot.model.ShoppingListItem
import ru.andchelp.money.tracker.bot.service.MessageService
import ru.andchelp.money.tracker.bot.service.ShoppingListService
import ru.andchelp.money.tracker.bot.service.UserService
import java.math.BigDecimal
import java.math.RoundingMode

@Configuration
class ShoppingListHandler(
    private val msgService: MessageService,
    private val shoppingListService: ShoppingListService,
    private val userService: UserService
) {
    @Bean("shopping_list_btn_text")
    fun shoppingListBtn() = GeneralTextMessageHandler { msg ->
        if (msg.text != TextKey.SHOPPING_LIST) return@GeneralTextMessageHandler

        msgService.send(
            "Ваш список покупок:",
            shoppingListService.getItemsKeyboard(msg.userId, "check_shopping_list_item")
                .row().button(TextKey.EDIT, "edit_shopping_list").button(TextKey.ADD, "add_shopping_list_item")
                .row().button(TextKey.OUTCOME, "new_outcome_shopping_list")
        )
    }

    @Bean("shopping_list_clbk")
    fun shoppingListClbk() = CallbackHandler { clbk ->
        renderShoppingList(clbk.msgId, clbk.userId)
    }

    private fun renderShoppingList(msgId: Int, userId: Long) {
        msgService.edit(
            msgId,
            "Ваш список покупок:",
            shoppingListService.getItemsKeyboard(userId, "check_shopping_list_item")
                .row().button(TextKey.EDIT, "edit_shopping_list").button(TextKey.ADD, "add_shopping_list_item")
                .row().button(TextKey.OUTCOME, "new_outcome_shopping_list")
        )
    }

    @Bean("add_shopping_list_item")
    fun addShoppingListItem() = CallbackHandler { clbk ->
        msgService.edit(
            clbk.msgId,
            "Добавление покупки\n\n" +
                    "Введите название",
            MsgKeyboard().row().button(TextKey.CANCEL, "shopping_list_clbk")
        )
        ContextHolder.set(ShoppingListItemContext(clbk.msgId, handlerId = "shopping_list_name_input"))
    }

    @Bean("shopping_list_name_input")
    fun shoppingListNameInput() = ContextualTextMessageHandler { msg ->
        val context: ShoppingListItemContext = ContextHolder.current()!!
        context.name = msg.text
        msgService.edit(
            context.baseMsgId,
            "Добавление покупки\n\n" +
                    "Название: ${context.name}\n" +
                    "Введите сумму покупки",
            MsgKeyboard().row().button(TextKey.CANCEL, "shopping_list_clbk")
        )
        context.handlerId = "shopping_list_sum_input"
        msgService.delete(msg.msgId)
    }

    @Bean("shopping_list_sum_input")
    fun shoppingListSumInput() = ContextualTextMessageHandler { msg ->
        val context: ShoppingListItemContext = ContextHolder.current()!!
        context.sum = BigDecimal(msg.text).setScale(2, RoundingMode.HALF_EVEN)
        msgService.edit(
            context.baseMsgId,
            "Добавление покупки\n\n" +
                    "Название: ${context.name}\n" +
                    "Сумма: ${context.sum}",
            MsgKeyboard().row().button(TextKey.CANCEL, "shopping_list_clbk")
                .button(TextKey.CONFIRM, "shopping_list_save_item_clbk")
        )
        msgService.delete(msg.msgId)
    }

    @Bean("shopping_list_save_item_clbk")
    fun shoppingListSaveItemClbk() = CallbackHandler { clbk ->
        val context: ShoppingListItemContext = ContextHolder.current()!!
        val user = userService.findById(clbk.userId)
        shoppingListService.save(ShoppingListItem(user = user, name = context.name, sum = context.sum))
        renderShoppingList(clbk.msgId, clbk.userId)
    }

    @Bean("check_shopping_list_item")
    fun checkShoppingListItem() = CallbackHandler { clbk ->
        val item = shoppingListService.findByIdAndUserId(clbk.data.toLong(), clbk.userId)
        item.bought = !item.bought
        shoppingListService.save(item)
        renderShoppingList(clbk.msgId, clbk.userId)
    }

    @Bean("edit_shopping_list")
    fun editShoppingList() = CallbackHandler { clbk ->
        renderEditShoppingList(clbk)
    }

    @Bean("edit_shopping_list_item")
    fun editShoppingListItem() = CallbackHandler { clbk ->
        val item = shoppingListService.findByIdAndUserId(clbk.data.toLong(), clbk.userId)
        msgService.edit(
            clbk.msgId,
            "Редактирование покупки",
            MsgKeyboard()
                .row().button("Название: ${item.name}", "edit_shopping_list_name")
                .row().button("Сумма: ${item.sum}", "edit_shopping_list_sum")
                .row().button(TextKey.BACK, "edit_shopping_list")
                .button(TextKey.DELETE, "delete_shopping_list_item", item.id)
        )
    }

    @Bean("delete_shopping_list_item")
    fun deleteShoppingListItem() = CallbackHandler { clbk ->
        shoppingListService.delete(clbk.data.toLong(), clbk.userId)
        renderEditShoppingList(clbk)
    }

    private fun renderEditShoppingList(clbk: CallbackUpdate) {
        msgService.edit(
            clbk.msgId,
            "Выберите покупку для изменения:",
            shoppingListService.getItemsKeyboard(clbk.userId, "edit_shopping_list_item")
                .row().button(TextKey.BACK, "shopping_list_clbk")
        )
    }

}