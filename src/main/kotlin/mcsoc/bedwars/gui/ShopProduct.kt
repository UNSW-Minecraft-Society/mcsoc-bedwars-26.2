package mcsoc.bedwars.gui

import eu.pb4.sgui.api.ClickType
import eu.pb4.sgui.api.elements.GuiElement
import mcsoc.bedwars.BedwarsPlugin
import mcsoc.bedwars.datatrackers.ModDataTracker
import mcsoc.bedwars.upgrades.UpgradeItemType
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemStackTemplate

abstract class ShopProduct {
    abstract fun getItemStack(): ItemStack
    abstract fun getClickCallback(): GuiElement.ClickCallback
    abstract fun getItemCost(): ItemStack

    // Handles purchasing logic, returns true if purchase successful.
    // transaction handles the effect of purchase (e.g. giving an item), returning false if it fails.
    protected fun purchaseUnit(player: Player, transaction: () -> Boolean, sendMsg: Boolean = true): Boolean {
        val inventory = player.inventory
        val currency = getItemCost().item
        val price = getItemCost().count
        if (inventory.countItem(currency) < price) {
            player.playSound(SoundEvents.NOTE_BLOCK_BIT.value())
            if (sendMsg) player.sendSystemMessage(Component.literal("Insufficient funds"))
            return false
        }
        if (transaction()) {
            inventory.clearOrCountMatchingItems({it.`is`(currency)},
                price, inventory)
            player.playSound(SoundEvents.NOTE_BLOCK_BELL.value())
            if (sendMsg) player.sendSystemMessage(Component.literal("Purchased ${getItemStack().toString()}"))
            return true
        } else {
            player.playSound(SoundEvents.NOTE_BLOCK_BIT.value())
            if (sendMsg) player.sendSystemMessage(Component.literal("Insufficient space in inventory"))
            return false
        }
    }
}

abstract class PlayerSpecificShopProduct : ShopProduct() {
    abstract fun setPlayer(player: ServerPlayer)
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

    override fun getClickCallback(): GuiElement.ClickCallback {
        return GuiElement.ClickCallback { index, clickType, action, gui ->
            val player = gui.player ?: return@ClickCallback
            val inventory = player.inventory
            BedwarsPlugin.LOGGER.info("item out: {}", getItemStack())
            if (clickType == ClickType.MOUSE_LEFT) {
                purchaseUnit(player, {inventory.add(getItemStack().copy())})
            } else if (clickType == ClickType.MOUSE_LEFT_SHIFT) {
                var count = 0
                while (purchaseUnit(player, {inventory.add(getItemStack().copy())}, false)) count++
                player.sendSystemMessage(Component.literal("Purchased ${getItemStack()} x${count}"))
            }
        }
    }

    override fun getItemCost(): ItemStack {
        return ItemStack(currency, price)
    }
}

class ShopPlayerUpgrade : PlayerSpecificShopProduct {
    private val player_upgrade: UpgradeItemType
    private val currency: Item
    private val price: Int
    private lateinit var player: ServerPlayer

    constructor(player_upgrade: UpgradeItemType, currency: Item, price: Int) {
        this.player_upgrade = player_upgrade
        this.currency = currency
        this.price = price
    }

    override fun getItemStack(): ItemStack {
        return ModDataTracker.getItemStack(player, player_upgrade)
    }

    override fun getClickCallback(): GuiElement.ClickCallback {
        return GuiElement.ClickCallback { index, clickType, action, gui ->
            val player = gui.player ?: return@ClickCallback
            purchaseUnit(player, fun(): Boolean {
                ModDataTracker.upgradeItem(player, player_upgrade)
                return true
            })
        }
    }

    override fun getItemCost(): ItemStack {
        TODO("Not yet implemented")
    }

    override fun setPlayer(player: ServerPlayer) {
        this.player = player
    }

}