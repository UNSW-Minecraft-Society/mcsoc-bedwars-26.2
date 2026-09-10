package mcsoc.bedwars.gui

import eu.pb4.sgui.api.ClickType
import eu.pb4.sgui.api.elements.GuiElement
import eu.pb4.sgui.api.elements.GuiElementBuilder
import eu.pb4.sgui.api.gui.SimpleGui
import mcsoc.bedwars.BedwarsPlugin
import mcsoc.bedwars.datatrackers.generatordata.InvalidTeamException
import mcsoc.bedwars.items.BedwarsItems
import mcsoc.bedwars.upgrades.TeamUpgradeType
import mcsoc.bedwars.upgrades.TrapUpgrade
import mcsoc.bedwars.upgrades.UpgradeItemType
import mcsoc.bedwars.utils.Team
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.Items
import net.minecraft.world.item.alchemy.Potions

enum class ShopType(val title: String) {
    PLAYER_SHOP("Player Shop"),
    TEAM_SHOP("Team Shop");
}

/**
 * Object containing logic pertaining to storing and displaying (via a GUI) items in the shop.
 */
object ShopGui {
    private val LOGGER = BedwarsPlugin.LOGGER

    // Slot order which the `PRODUCTS` appear in.
    private val PRODUCT_SLOT_INDEX = arrayOf(
        1, 10, 19, 28, 37,
        2, 11, 20, 29, 38,
        3, 12, 21, 30, 39,
        4, 13, 22, 31, 40,
        5, 14, 23, 32, 41,
        6, 15, 24, 33, 42,
        7, 16, 25, 34, 43)

    private val PRODUCTS: Map<ShopType,Array<ShopProduct>> = getProducts()

    /**
     * Fetches a list of the ShopProducts.
     */
    private fun getProducts(): Map<ShopType,Array<ShopProduct>> {
        // At some point put this into a config file to be read, instead of hard-coded
        return mapOf(
             ShopType.PLAYER_SHOP to arrayOf(
                 // These are ShopPlayerUpgrades
                 ShopPlayerUpgrade(UpgradeItemType.ARMOUR,
                     arrayOf(Items.GOLD_INGOT, Items.DIAMOND, Items.EMERALD),
                     arrayOf(3, 4, 5),
                     arrayOf("Chainmail Armor", "Iron Armor", "Diamond Armor")
                 ),
                 ShopPlayerUpgrade(UpgradeItemType.SWORD,
                     arrayOf(Items.IRON_INGOT, Items.GOLD_INGOT, Items.DIAMOND),
                     arrayOf(3, 4, 5),
                     arrayOf("Stone Sword", "Iron Sword", "Diamond Sword")
                 ),
                 ShopPlayerUpgrade(UpgradeItemType.PICKAXE,
                     arrayOf(Items.IRON_INGOT, Items.GOLD_INGOT, Items.DIAMOND, Items.EMERALD),
                     arrayOf(1, 2, 3, 4),
                     arrayOf("Wooden Pickaxe", "Iron Pickaxe", "Golden Pickaxe", "Diamond Pickaxe")
                 ),
                 ShopPlayerUpgrade(UpgradeItemType.AXE,
                     arrayOf(Items.IRON_INGOT, Items.GOLD_INGOT, Items.DIAMOND, Items.EMERALD),
                     arrayOf(1, 2, 3, 4),
                     arrayOf("Wooden Axe", "Stone Axe", "Iron Axe", "Diamond Axe")
                 ),
                 EmptyShopProduct(),

                 // These are ShopItems
                 ShopItem(Items.SHEARS, 1, Items.IRON_INGOT, 15),
                 ShopPlayerCustomItem({player -> BedwarsItems.knockbackStickItemStack(player.level())}, Items.GOLD_INGOT, 5),
                 ShopItem(Items.WATER_BUCKET, 1, Items.GOLD_INGOT, 6),
                 EmptyShopProduct(),
                 EmptyShopProduct(),

                 ShopItem(Items.ARROW, 16, Items.GOLD_INGOT, 2),
                 ShopItem(Items.BOW, 1, Items.GOLD_INGOT, 12),
                 ShopPlayerCustomItem({player -> BedwarsItems.powerBowItemStack(player.level())}, Items.GOLD_INGOT, 24),
                 ShopPlayerCustomItem({player -> BedwarsItems.punchBowItemStack(player.level())}, Items.EMERALD, 6),
                 EmptyShopProduct(),

                 ShopTeamItem(Team.entries.associateWith { Items.WOOL.pick(it.dyeColour) },
                     16, Items.IRON_INGOT, 4),
                 ShopItem(Items.SANDSTONE, 16, Items.IRON_INGOT, 16),
                 ShopItem(Items.END_STONE, 12, Items.IRON_INGOT, 24),
                 ShopItem(Items.OBSIDIAN, 4, Items.EMERALD, 4),
                 ShopItem(Items.OAK_PLANKS, 16, Items.GOLD_INGOT, 6),

                 ShopItem(Items.LADDER, 16, Items.IRON_INGOT, 4),
                 ShopItem(Items.WIND_CHARGE, 1, Items.GOLD_INGOT, 24),
                 ShopCustomItem(BedwarsItems::popupTowerItemStack, Items.GOLD_INGOT, 24),
                 ShopCustomItem(BedwarsItems::bridgeEggItemStack, Items.EMERALD, 1),
                 ShopItem(Items.ENDER_PEARL, 1, Items.EMERALD, 4),

                 ShopCustomItem(BedwarsItems::ballOfBugsItemStack, Items.GOLD_INGOT, 2),
                 ShopCustomItem(BedwarsItems::fireballItemStack, Items.IRON_INGOT, 36),
                 ShopCustomItem(BedwarsItems::instantTNTItemStack, Items.GOLD_INGOT, 8),
                 ShopItem(Items.IRON_GOLEM_SPAWN_EGG, 2, Items.IRON_INGOT, 120),
                 EmptyShopProduct(),

                 ShopItem(Items.GOLDEN_APPLE, 1, Items.GOLD_INGOT, 3),
                 ShopCustomItem({BedwarsItems.potionItemStack(Potions.LEAPING)}, Items.EMERALD, 1),
                 ShopCustomItem({BedwarsItems.potionItemStack(Potions.SWIFTNESS)}, Items.EMERALD, 1),
                 ShopCustomItem({BedwarsItems.potionItemStack(Potions.INVISIBILITY)}, Items.EMERALD, 2),
                 EmptyShopProduct(),
             ),
             ShopType.TEAM_SHOP to arrayOf(
                 EmptyShopProduct(),
                 EmptyShopProduct(),
                 EmptyShopProduct(),
                 EmptyShopProduct(),
                 EmptyShopProduct(),

                 EmptyShopProduct(),
                 IntShopTeamUpgrade(TeamUpgradeType.PROTECTION, Items.SHIELD,
                     Array(4) {Items.DIAMOND},
                     arrayOf(5,10,20,30),
                     "Protection"
                 ).addDescriptionLine("Applies protection to your team's armour for more defense"),
                 IntShopTeamUpgrade(TeamUpgradeType.FEATHER_FALLING, Items.FEATHER,
                     Array(2) {Items.DIAMOND},
                     arrayOf(1,2),
                     "Feather Falling"
                 ).addDescriptionLine("Applies feather falling to your team's boots for less fall damage"),
                 IntShopTeamUpgrade(TeamUpgradeType.HASTE, Items.GOLDEN_PICKAXE,
                     Array(2) {Items.DIAMOND},
                     arrayOf(2,3),
                     "Haste"
                 ).addDescriptionLine("Applies haste to your team for faster block breaking"),
                 EmptyShopProduct(),

                 EmptyShopProduct(),
                 BooleanShopTeamUpgrade(TeamUpgradeType.SHARPNESS, Items.IRON_SWORD, Items.DIAMOND, 8, "Sharpness")
                     .addDescriptionLine("Applies sharpness to your team's swords to deal more damage"),
                 BooleanShopTeamUpgrade(TeamUpgradeType.HEAL_POOL, Items.GOLDEN_APPLE, Items.DIAMOND, 3, "Heal Pool")
                     .addDescriptionLine("Applies faster healing for your team at your island"),
                 EmptyShopProduct(),
                 EmptyShopProduct(),

                 EmptyShopProduct(),
                 EmptyShopProduct(),
                 EmptyShopProduct(),
                 EmptyShopProduct(),
                 EmptyShopProduct(),

                 EmptyShopProduct(),
                 ShopTrapUpgrade(TrapUpgrade.BLINDNESS, Items.DYE.black, Items.DIAMOND, 2, "Blindness Trap")
                     .addDescriptionLine("Applies blindness to an intruder on your island"),
                 ShopTrapUpgrade(TrapUpgrade.COUNTER, Items.POTION, Items.DIAMOND, 2, "Counter Trap")
                     .addDescriptionLine("Applies buffs to your team when an intruder enters your island"),
                 EmptyShopProduct(),
                 EmptyShopProduct(),

                 EmptyShopProduct(),
                 ShopTrapUpgrade(TrapUpgrade.REVEAL, Items.ENDER_EYE, Items.DIAMOND, 2, "Reveal Trap"),
                 ShopTrapUpgrade(TrapUpgrade.MINING, Items.ELDER_GUARDIAN_SPAWN_EGG, Items.DIAMOND, 2, "Mining Fatigue Trap"),
                 EmptyShopProduct(),
                 EmptyShopProduct(),

                 EmptyShopProduct(),
                 EmptyShopProduct(),
                 EmptyShopProduct(),
                 EmptyShopProduct(),
                 EmptyShopProduct(),
             )
        )
    }

    /**
     * Displays the shop GUI to the provided player. Returns 1 if succeeded.
     */
    fun displayShop(player: ServerPlayer, shopType: ShopType) {
        try {
            LOGGER.info("Displaying shop gui")
            /**
             * Update items within the shop gui.
             */
            fun updateItems(gui: SimpleGui) {
                val products = PRODUCTS[shopType]
                if (products !is Array<ShopProduct>) return
                for ((slotIndex, product) in PRODUCT_SLOT_INDEX zip products) {
                    if (product is PlayerSpecificShopProduct) product.setShopPlayer(player)
                    val element = GuiElementBuilder(product.getItemStack())
                        .setCallback(product.getClickCallback())
                    if (product.getProductName() != null) element.setName(product.getProductName())
                    for (line in product.getDescriptionLines()) element.addLoreLine(line)
                    element.addLoreLine(Component.literal("Cost: ${product.getItemCost()?.count} ").append(product.getItemCost()?.hoverName ?: Component.empty()))
                    gui.setSlot(slotIndex, element)
                }
            }

            val gui = object : SimpleGui(MenuType.GENERIC_9x5, player, false) {
                override fun onClick(
                    index: Int,
                    type: ClickType?,
                    action: ContainerInput?,
                    element: GuiElement?
                ): Boolean {
                    this.player.sendSystemMessage(Component.literal(type.toString()), false)
                    updateItems(this)
                    return super.onClick(index, type, action, element)
                }
            }

            updateItems(gui)
            gui.title = Component.literal(shopType.title)
            gui.open()
        } catch (e: InvalidTeamException) {
            player.sendSystemMessage(Component.literal("You must be on a team to open the shop"))
        }
    }
}