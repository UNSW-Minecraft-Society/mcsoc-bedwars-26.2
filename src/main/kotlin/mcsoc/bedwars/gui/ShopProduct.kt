package mcsoc.bedwars.gui

import eu.pb4.sgui.api.ClickType
import eu.pb4.sgui.api.elements.GuiElement
import mcsoc.bedwars.BedwarsPlugin
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemStackTemplate

interface ShopProduct {
    fun getItemStack(): ItemStack
    fun getClickCallback(): GuiElement.ClickCallback
    fun getItemCost(): ItemStack
}

class ShopItem : ShopProduct {
    private val item_template: ItemStackTemplate
    private lateinit var item: ItemStack
    private val currency: Item
    private val price: Int

    constructor(template: ItemStackTemplate, currency: Item, price: Int) {
        this.item_template = template
        this.currency = currency
        this.price = price
    }
    constructor(item: Item, count: Int, currency: Item, price: Int) : this(ItemStackTemplate(item, count),
        currency, price)

    private fun resolveItemStackTemplate(): ItemStack {
        if (!this::item.isInitialized) {
            BedwarsPlugin.LOGGER.info("creating")
            this.item = item_template.create()
        }
        return this.item.copy()
    }

    override fun getItemStack(): ItemStack {
        return resolveItemStackTemplate()
    }

    private fun purchaseUnit(player: Player, sendMsg: Boolean = true): Boolean {
        val inventory = player.inventory
        if (inventory.countItem(currency) < price) {
            player.playSound(SoundEvents.NOTE_BLOCK_BIT.value())
            if (sendMsg) player.sendSystemMessage(Component.literal("Insufficient funds"))
            return false
        }
        if (inventory.add(getItemStack().copy())) {
            inventory.clearOrCountMatchingItems({it.`is`(currency)},
                price, inventory)
            player.playSound(SoundEvents.NOTE_BLOCK_BELL.value())
            if (sendMsg) player.sendSystemMessage(Component.literal("Purchased " + getItemStack().toString()))
            return true
        } else {
            player.playSound(SoundEvents.NOTE_BLOCK_BIT.value())
            if (sendMsg) player.sendSystemMessage(Component.literal("Insufficient space in inventory"))
            return false
        }
    }

    override fun getClickCallback(): GuiElement.ClickCallback {
        return GuiElement.ClickCallback { index, clickType, action, gui ->
            val player = gui.player ?: return@ClickCallback
            val inventory = player.inventory
            BedwarsPlugin.LOGGER.info("item out: {}", getItemStack())
            if (clickType == ClickType.MOUSE_LEFT) {
                purchaseUnit(player)
            } else if (clickType == ClickType.MOUSE_LEFT_SHIFT) {
                var count = 0
                while (purchaseUnit(player, false)) count++
                player.sendSystemMessage(Component.literal("Purchased " + getItemStack().toString()
                        + " x" + count))
            }
        }
    }

    override fun getItemCost(): ItemStack {
        return ItemStack(currency, price)
    }
}