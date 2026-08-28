package mcsoc.bedwars.gui

import eu.pb4.sgui.api.ClickType
import eu.pb4.sgui.api.elements.GuiElement
import mcsoc.bedwars.BedwarsPlugin
import mcsoc.bedwars.datatrackers.ModDataTracker
import mcsoc.bedwars.upgrades.UpgradeItemType
import mcsoc.bedwars.utils.Team
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemStackTemplate
import net.minecraft.world.item.Items
import kotlin.uuid.toKotlinUuid

val DEFAULT_TEAM = Team.BLACK

/**
 * Abstract class for storing data on shop products.
 */
abstract class ShopProduct {
    /**
     * Gets the `ItemStack` to display in the shop menu.
     */
    abstract fun getItemStack(): ItemStack

    /**
     * Gets the callback function to be executed when this item is clicked in the shop.
     */
    abstract fun getClickCallback(): GuiElement.ClickCallback
    abstract fun getItemCost(): ItemStack

    /**
     * Handles purchasing logic, returns true if purchase successful.
     * transaction is a function that handles the effect of purchase (e.g. giving an item), returning false if it fails.
      */
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
            if (sendMsg) player.sendSystemMessage(Component.literal("Transaction failed"))
            return false
        }
    }
}

/**
 * Interface for the functionality to store data used by player-specific shop products (e.g. team colored blocks,
 * player-specific upgrades). `setPlayer` needs to be called to initialize the player it before this class is used.
 */
interface PlayerSpecificShopProduct {
    fun setPlayer(player: ServerPlayer)
}

/**
 * Class for storing data on default item shop products.
 */
open class ShopItem : ShopProduct {
    private var itemTemplate: ItemStackTemplate
    private lateinit var stack: ItemStack
    private val currency: Item
    private val price: Int

    constructor(template: ItemStackTemplate, currency: Item, price: Int) {
        this.itemTemplate = template
        this.currency = currency
        this.price = price
    }
    constructor(item: Item, count: Int, currency: Item, price: Int) : this(ItemStackTemplate(item, count),
        currency, price)

    private fun resolveItemStackTemplate(): ItemStack {
        if (!this::stack.isInitialized) {
            BedwarsPlugin.LOGGER.info("creating")
            this.stack = itemTemplate.create()
        }
        return this.stack.copy()
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

    protected fun setItemStack(stack: ItemStack) {
        this.itemTemplate = ItemStackTemplate(stack.item, stack.count)
        this.stack = stack
    }

    protected fun setItemStack(itemTemplate: ItemStackTemplate) {
        if (!this::stack.isInitialized) this.itemTemplate = itemTemplate
        else setItemStack(itemTemplate.create())
    }

}

class ShopTeamItem : ShopItem, PlayerSpecificShopProduct {
    private val templates: Map<Team, ItemStackTemplate>

    constructor(templates: Map<Team, ItemStackTemplate>, currency: Item, price: Int) : super(
        templates.get(Team.NONE) ?: ItemStackTemplate(Items.BARRIER), currency, price) {
        this.templates = templates
    }

    constructor(items: Map<Team, Item>, count: Int, currency: Item, price: Int) : this(
        items.mapValues { ItemStackTemplate(it.value, count) },currency, price)

    override fun setPlayer(player: ServerPlayer) {
        val team = ModDataTracker.getPlayersTeam(player.uuid.toKotlinUuid())
        setItemStack(templates.getValue(team))
    }
}

/**
 * Class for storing data on player upgrades (e.g. tool and armor material upgrades).
 */
class ShopPlayerUpgrade : ShopProduct, PlayerSpecificShopProduct {
    private val playerUpgrade: UpgradeItemType
    private val currencies: Array<Item>
    private val prices: Array<Int>
    private lateinit var player: ServerPlayer

    constructor(playerUpgrade: UpgradeItemType, currencies: Array<Item>, prices: Array<Int>) {
        this.playerUpgrade = playerUpgrade
        this.currencies = currencies
        this.prices = prices
    }

    override fun getItemStack(): ItemStack {
        return ModDataTracker.getNextItemStack(player, playerUpgrade) ?: Items.STAINED_GLASS_PANE.lightGray.defaultInstance
    }

    override fun getClickCallback(): GuiElement.ClickCallback {
        return GuiElement.ClickCallback { index, clickType, action, gui ->
            val player = gui.player ?: return@ClickCallback
            purchaseUnit(player, fun(): Boolean {
                ModDataTracker.upgradeItem(player, playerUpgrade)
                return true
            })
        }
    }

    override fun getItemCost(): ItemStack {
        val tier = ModDataTracker.getTier(player, playerUpgrade)
        if (tier >= currencies.size) return ItemStack(Items.BARRIER, 999)
        return ItemStack(currencies[tier], prices[tier])
    }

    override fun setPlayer(player: ServerPlayer) {
        this.player = player
    }

}