package mcsoc.bedwars.gui

import eu.pb4.sgui.api.ClickType
import eu.pb4.sgui.api.elements.GuiElement
import mcsoc.bedwars.BedwarsPlugin
import mcsoc.bedwars.datatrackers.gameState
import mcsoc.bedwars.upgrades.TeamUpgradeType
import mcsoc.bedwars.upgrades.TrapUpgrade
import mcsoc.bedwars.upgrades.UpgradeItemType
import mcsoc.bedwars.utils.Team
import mcsoc.bedwars.utils.romanNumeralMap
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemStackTemplate
import net.minecraft.world.item.Items

val DEFAULT_TEAM = Team.BLACK
val EMPTY_STACK = Items.AIR.defaultInstance

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
    abstract fun getItemCost(): ItemStack?

    abstract fun getProductName(): Component?

    /**
     * Handles purchasing logic, returns true if purchase successful.
     * transaction is a function that handles the effect of purchase (e.g. giving an item), returning false if it fails.
      */
    protected fun purchaseUnit(player: Player, transaction: () -> Boolean, sendMsg: Boolean = true): Boolean {
        val inventory = player.inventory
        val currency = getItemCost()?.item ?: return false
        val price = getItemCost()?.count ?: return false
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

class EmptyShopProduct : ShopProduct() {
    override fun getItemStack(): ItemStack = EMPTY_STACK

    override fun getClickCallback(): GuiElement.ClickCallback = GuiElement.ClickCallback {
        index, clickType, action, gui ->
    }

    override fun getItemCost(): ItemStack? = null

    override fun getProductName(): Component? = null
}

/**
 * Interface for the functionality to store data used by player-specific shop products (e.g. team colored blocks,
 * player-specific upgrades). `setPlayer` needs to be called to initialize the player it before this class is used.
 */
interface PlayerSpecificShopProduct {
    fun setShopPlayer(player: ServerPlayer)
}

/**
 * Class for storing data on default item shop products.
 */
abstract class AbstractShopItem : ShopProduct {
    protected val currency: Item
    protected val price: Int

    constructor(currency: Item, price: Int) {
        this.currency = currency
        this.price = price
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

    override fun getProductName(): Component {
        return getItemStack().hoverName
    }
}

open class ShopItem : AbstractShopItem {
    protected lateinit var stack: ItemStack
    protected var itemTemplate: ItemStackTemplate

    constructor(template: ItemStackTemplate, currency: Item, price: Int) : super(currency, price) {
        this.itemTemplate = template
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

    protected fun setItemStack(stack: ItemStack) {
        this.itemTemplate = ItemStackTemplate(stack.item, stack.count)
        this.stack = stack
    }

    protected fun setItemStack(itemTemplate: ItemStackTemplate) {
        if (!this::stack.isInitialized) this.itemTemplate = itemTemplate
        else setItemStack(itemTemplate.create())
    }
}

class ShopCustomItem : AbstractShopItem {
    protected var stackCreate: () -> ItemStack

    constructor(stackCreate: () -> ItemStack, currency: Item, price: Int) : super(currency, price) {
        this.stackCreate = stackCreate
    }

    override fun getItemStack() = stackCreate()
}


class ShopPlayerCustomItem : AbstractShopItem, PlayerSpecificShopProduct {
    protected var stackCreate: (ServerPlayer) -> ItemStack
    private lateinit var player: ServerPlayer

    constructor(stackCreate: (ServerPlayer) -> ItemStack, currency: Item, price: Int) : super(currency, price) {
        this.stackCreate = stackCreate
    }

    override fun getItemStack() = stackCreate(player)

    override fun setShopPlayer(player: ServerPlayer) {
        this.player = player
    }
}

class ShopTeamItem : ShopItem, PlayerSpecificShopProduct {
    private val templates: Map<Team, ItemStackTemplate>
    private lateinit var player: ServerPlayer

    constructor(templates: Map<Team, ItemStackTemplate>, currency: Item, price: Int) : super(
        templates[Team.NONE] ?: ItemStackTemplate(Items.BARRIER), currency, price) {
        this.templates = templates
    }

    constructor(items: Map<Team, Item>, count: Int, currency: Item, price: Int) : this(
        items.mapValues { ItemStackTemplate(it.value, count) },currency, price)

    override fun setShopPlayer(player: ServerPlayer) {
        val gameState = player.level().gameState
        val team = gameState.getPlayersTeam(player.uuid)
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
    private val names: Array<String>

    constructor(playerUpgrade: UpgradeItemType, currencies: Array<Item>, prices: Array<Int>, names: Array<String>) : super() {
        this.playerUpgrade = playerUpgrade
        this.currencies = currencies
        this.prices = prices
        this.names = names
    }

    override fun getItemStack(): ItemStack {
        val gameState = player.level().gameState
        return gameState.getNextItemStack(player, playerUpgrade) ?: EMPTY_STACK
    }

    override fun getClickCallback(): GuiElement.ClickCallback {
        return GuiElement.ClickCallback { index, clickType, action, gui ->
            val player = gui.player ?: return@ClickCallback
            val gameState = player.level().gameState
            purchaseUnit(player, fun(): Boolean {
                gameState.upgradeItem(player, playerUpgrade)
                return true
            })
        }
    }

    override fun getItemCost(): ItemStack? {
        val gameState = player.level().gameState
        val tier = gameState.getTier(player, playerUpgrade)
        return if (tier >= currencies.size) null
        else  ItemStack(currencies[tier], prices[tier])
    }

    override fun getProductName(): Component? {
        val gameState = player.level().gameState
        val tier = gameState.getTier(player, playerUpgrade)
        return if (tier >= currencies.size) null
        else Component.literal(names[tier])
    }

    override fun setShopPlayer(player: ServerPlayer) {
        this.player = player
    }

}

abstract class ShopTeamUpgrade<T> : ShopProduct, PlayerSpecificShopProduct {
    protected var teamUpgrade: TeamUpgradeType<T>
    protected var displayItem: Item
    private lateinit var player: ServerPlayer

    constructor(teamUpgrade: TeamUpgradeType<T>, displayItem: Item) {
        this.teamUpgrade = teamUpgrade
        this.displayItem = displayItem
    }

    override fun getClickCallback(): GuiElement.ClickCallback {
        return GuiElement.ClickCallback { index, clickType, action, gui ->
            val player = gui.player ?: return@ClickCallback
            if (!isUpgradable()) return@ClickCallback
            val gameState = player.level().gameState
            val team = gameState.getPlayersTeam(player.uuid)
            purchaseUnit(player, fun(): Boolean {
                gameState.upgrade(team, teamUpgrade)
                return true
            })
        }
    }

    override fun setShopPlayer(player: ServerPlayer) {
        this.player = player
    }

    protected fun getUpgradeState(): T {
        val gameState = player.level().gameState
        val team = gameState.getPlayersTeam(player.uuid)
        return gameState.getUpgrade(team, teamUpgrade)
    }

    protected abstract fun isUpgradable(): Boolean
}

class BooleanShopTeamUpgrade : ShopTeamUpgrade<Boolean> {
    private var currency: Item
    private var price: Int
    private val name: Component

    constructor(teamUpgrade: TeamUpgradeType<Boolean>, displayItem: Item, currency: Item, price: Int, name: Component) : super(teamUpgrade, displayItem) {
        this.currency = currency
        this.price = price
        this.name = name
    }

    constructor(teamUpgrade: TeamUpgradeType<Boolean>, displayItem: Item, currency: Item, price: Int, name: String) : this(
        teamUpgrade, displayItem, currency, price, Component.literal(name)
    )

    override fun getItemStack(): ItemStack {
        return if (!getUpgradeState())
            ItemStack(displayItem)
        else
            EMPTY_STACK
    }

    override fun getItemCost(): ItemStack? {
        return if (isUpgradable()) ItemStack(currency, price)
        else null
    }

    override fun getProductName(): Component? {
        return if (isUpgradable()) name
        else null
    }

    override fun isUpgradable(): Boolean = !getUpgradeState()

}

class IntShopTeamUpgrade : ShopTeamUpgrade<Int> {
    private val currencies: Array<Item>
    private val prices: Array<Int>
    private val baseName: String

    constructor(teamUpgrade: TeamUpgradeType<Int>, displayItem: Item, currencies: Array<Item>, prices: Array<Int>, baseName: String) : super(teamUpgrade, displayItem) {
        this.currencies = currencies
        this.prices = prices
        this.baseName = baseName
    }

    override fun getItemStack(): ItemStack {
        if (!isUpgradable()) return EMPTY_STACK
        return ItemStack(displayItem, getUpgradeState() + 1)
    }

    override fun getItemCost(): ItemStack? {
        val nextTier = getUpgradeState()
        return currencies.getOrNull(nextTier)?.let { prices.getOrNull(nextTier)?.let { count -> ItemStack(it, count) } }
    }

    override fun getProductName(): Component? {
        return if (!isUpgradable()) null
        else Component.literal(baseName + romanNumeralMap[getUpgradeState() + 1])
    }

    override fun isUpgradable(): Boolean = currencies.lastIndex >= getUpgradeState()

}

class ShopTrapUpgrade : ShopProduct, PlayerSpecificShopProduct {
    private val trapUpgrade: TrapUpgrade
    private val displayItem: Item
    private val currency: Item
    private val price: Int
    private lateinit var player: ServerPlayer
    private val name: Component

    constructor(trapUpgrade: TrapUpgrade, displayItem: Item, currency: Item, price: Int, name: Component) : super() {
        this.trapUpgrade = trapUpgrade
        this.displayItem = displayItem
        this.currency = currency
        this.price = price
        this.name = name
    }

    constructor(trapUpgrade: TrapUpgrade, displayItem: Item, currency: Item, price: Int, name: String) : this(
        trapUpgrade, displayItem, currency, price, Component.literal(name)
    )

    override fun getItemStack(): ItemStack {
        return if (!isTrapActive()) ItemStack(displayItem)
        else EMPTY_STACK
    }

    override fun getClickCallback(): GuiElement.ClickCallback {
        return GuiElement.ClickCallback { index, clickType, action, gui ->
            val player = gui.player ?: return@ClickCallback
            if (isTrapActive()) return@ClickCallback
            val gameState = player.level().gameState
            val team = gameState.getPlayersTeam(player.uuid)
            purchaseUnit(player, fun(): Boolean {
                gameState.addTrap(team, trapUpgrade)
                return true
            })
        }
    }

    override fun getItemCost(): ItemStack? {
        return if (!isTrapActive()) ItemStack(currency, price)
        else null
    }

    override fun getProductName(): Component? {
        return if (isTrapActive()) null
        else name
    }

    override fun setShopPlayer(player: ServerPlayer) {
        this.player = player
    }

    private fun isTrapActive(): Boolean {
        val gameState = player.level().gameState
        val team = gameState.getPlayersTeam(player.uuid)
        return gameState.getTraps(team).contains(trapUpgrade)
    }

}