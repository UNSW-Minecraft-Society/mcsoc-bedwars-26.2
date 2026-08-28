package mcsoc.bedwars.gui

import com.mojang.brigadier.context.CommandContext
import eu.pb4.sgui.api.ClickType
import eu.pb4.sgui.api.elements.AnimatedGuiElement
import eu.pb4.sgui.api.elements.AnimatedGuiElementBuilder
import eu.pb4.sgui.api.elements.GuiElement
import eu.pb4.sgui.api.elements.GuiElementBuilder
import eu.pb4.sgui.api.elements.SimpleGuiElement
import eu.pb4.sgui.api.gui.SimpleGui
import mcsoc.bedwars.BedwarsPlugin
import mcsoc.bedwars.upgrades.UpgradeItemType
import mcsoc.bedwars.utils.Team
import net.minecraft.ChatFormatting
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.Items
import net.minecraft.world.item.enchantment.Enchantments
import java.util.UUID

/**
 * Object containing logic pertaining to storing and displaying (via a GUI) items in the shop.
 */
object ShopGui {
    private val LOGGER = BedwarsPlugin.LOGGER

    // Slot order which the `PRODUCTS` appear in.
    private val PRODUCT_SLOT_INDEX = arrayOf(
        1, 10, 19, 28,
        2, 11, 20, 29,
        3, 12, 21, 30,
        4, 13, 22, 31,
        5, 14, 23, 32,
        6, 15, 24, 33,
        7, 16, 25, 34)

    private val PRODUCTS: Array<ShopProduct> = getProducts()

    /**
     * Fetches a list of the ShopProducts.
     */
    private fun getProducts(): Array<ShopProduct> {
        // At some point put this into a config file to be read, instead of hard-coded
        return arrayOf(
            // These are ShopPlayerUpgrades
            ShopPlayerUpgrade(UpgradeItemType.ARMOUR,
                arrayOf(Items.GOLD_INGOT, Items.DIAMOND, Items.EMERALD),
                arrayOf(3, 4, 5)),
            ShopPlayerUpgrade(UpgradeItemType.SWORD,
                arrayOf(Items.IRON_INGOT, Items.GOLD_INGOT, Items.DIAMOND),
                arrayOf(3, 4, 5)),
            ShopPlayerUpgrade(UpgradeItemType.PICKAXE,
                arrayOf(Items.IRON_INGOT, Items.GOLD_INGOT, Items.DIAMOND, Items.EMERALD),
                arrayOf(1, 2, 3, 4)),
            ShopPlayerUpgrade(UpgradeItemType.AXE,
                arrayOf(Items.IRON_INGOT, Items.GOLD_INGOT, Items.DIAMOND, Items.EMERALD),
                arrayOf(1, 2, 3, 4)),


            // These are ShopItems
            ShopItem(Items.SHEARS, 1, Items.IRON_INGOT, 30),
            ShopItem(Items.STICK, 1, Items.GOLD_INGOT, 10),
            ShopItem(Items.STAINED_GLASS_PANE.lightGray, 1, Items.BARRIER, 999),
            ShopItem(Items.STAINED_GLASS_PANE.lightGray, 1, Items.BARRIER, 999),

            ShopItem(Items.ARROW, 16, Items.GOLD_INGOT, 2),
            ShopItem(Items.BOW, 1, Items.GOLD_INGOT, 12),
            ShopItem(Items.BOW, 1, Items.GOLD_INGOT, 24),
            ShopItem(Items.BOW, 1, Items.EMERALD, 6),

            ShopTeamItem(Team.entries.associateWith { Items.WOOL.pick(it.dyeColour) },
                16, Items.IRON_INGOT, 4),
            ShopItem(Items.SANDSTONE, 16, Items.IRON_INGOT, 16),
            ShopItem(Items.END_STONE, 12, Items.IRON_INGOT, 24),
            ShopItem(Items.STAINED_GLASS_PANE.lightGray, 1, Items.BARRIER, 999),

            ShopItem(Items.OBSIDIAN, 4, Items.EMERALD, 4),
            ShopItem(Items.LADDER, 16, Items.IRON_INGOT, 16),
            ShopItem(Items.OAK_PLANKS, 16, Items.GOLD_INGOT, 6),
            ShopItem(Items.STAINED_GLASS_PANE.lightGray, 1, Items.BARRIER, 999),

            ShopItem(Items.GOLDEN_APPLE, 1, Items.GOLD_INGOT, 3),
            ShopItem(Items.IRON_GOLEM_SPAWN_EGG, 2, Items.IRON_INGOT, 150),
            ShopItem(Items.ENDER_PEARL, 1, Items.EMERALD, 4),
            ShopItem(Items.WATER_BUCKET, 1, Items.EMERALD, 2),

            ShopItem(Items.SPLASH_POTION, 1, Items.EMERALD, 1),
            ShopItem(Items.SPLASH_POTION, 1, Items.EMERALD, 1),
            ShopItem(Items.SPLASH_POTION, 1, Items.EMERALD, 1),
            ShopItem(Items.STAINED_GLASS_PANE.lightGray, 1, Items.BARRIER, 999),
        )
    }

    /**
     * Displays the shop GUI to the provided player. Returns 0 if succeeded.
     */
    fun displayShop(player: ServerPlayer): Int {
        try {
            LOGGER.info("Displaying shop gui")

            /**
             * Update items within the shop gui.
             */
            fun updateItems(gui: SimpleGui) {
                for ((slot_index, product) in PRODUCT_SLOT_INDEX zip PRODUCTS) {
                    if (product is PlayerSpecificShopProduct) product.setPlayer(player)
                    gui.setSlot(slot_index, GuiElementBuilder(product.getItemStack())
                        .addLoreLine(Component.literal("Cost: ${product.getItemCost()}"))
                        .setCallback(product.getClickCallback()))
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
            gui.title = Component.literal("Shop")
            gui.open()
        } catch (e: Exception) {
            LOGGER.error(e.stackTraceToString())
            e.printStackTrace()
        }
        return 0
    }

    /**
     * Displays the shop GUI to the provided player. Returns 0 if succeeded.
     */
    fun displayShop(objectCommandContext: CommandContext<CommandSourceStack>): Int {
        try {
            return displayShop(objectCommandContext.source.playerOrException)
        } catch (e: Exception) {
            LOGGER.error(e.stackTraceToString())
            e.printStackTrace()
            return 1
        }
    }

    /**
     * Test GUIs from Sgui translated to kotlin
     */
    fun testSimpleGui(objectCommandContext: CommandContext<CommandSourceStack>): Int {
        try {
            LOGGER.info("Testing simple gui")
            val player = objectCommandContext.source.player

            val gui = object : SimpleGui(MenuType.GENERIC_3x3, player, false) {
                override fun onClick(
                    index: Int,
                    type: ClickType?,
                    action: ContainerInput?,
                    element: GuiElement?
                ): Boolean {
                    this.player.sendSystemMessage(Component.literal(type.toString()), false)

                    return super.onClick(index, type, action, element)
                }

                override fun onTick() {
                    this.setSlot(
                        0, GuiElementBuilder(Items.ARROW)
                            .setCount((player?.level()?.gameTime?.rem(99))?.toInt() ?: 0).setMaxCount(99)
                    )
                    super.onTick()
                }
            }

            gui.title = Component.literal("Nice")
            gui.setSlot(0, GuiElementBuilder(Items.ARROW).setCount(99).setMaxDamage(99))
            gui.setSlot(
                1, AnimatedGuiElement(
                    arrayOf(
                        Items.NETHERITE_PICKAXE.defaultInstance,
                        Items.DIAMOND_PICKAXE.defaultInstance,
                        Items.GOLDEN_PICKAXE.defaultInstance,
                        Items.IRON_PICKAXE.defaultInstance,
                        Items.STONE_PICKAXE.defaultInstance,
                        Items.WOODEN_PICKAXE.defaultInstance
                    ), 10, false
                ) { p0, p1, p2, p3 -> {} }
            )
            gui.setSlot(
                2, AnimatedGuiElementBuilder()
                    .setItem(Items.NETHERITE_AXE).setDamage(150).saveItemStack()
                    .setItem(Items.DIAMOND_AXE).setDamage(150).unbreakable().saveItemStack()
                    .setItem(Items.GOLDEN_AXE).glow().saveItemStack()
                    .setItem(Items.IRON_AXE).enchant(
                        objectCommandContext.source.registryAccess(),
                        Enchantments.AQUA_AFFINITY, 1
                    ).hideDefaultTooltip().saveItemStack()
                    .setItem(Items.WOODEN_AXE).saveItemStack()
                    .setInterval(10).setRandom(true)
            )
            for (x in 3..gui.size - 1) {
                val itemStack = Items.STONE.defaultInstance
                itemStack.count = x
                gui.setSlot(
                    x, SimpleGuiElement(
                        itemStack
                    ) { p0, p1, p2, p3 -> {} }
                )
            }
            gui.setSlot(
                5, GuiElementBuilder(Items.PLAYER_HEAD)
                    .setProfileSkinTexture("ewogICJ0aW1lc3RhbXAiIDogMTYxOTk3MDIyMjQzOCwKICAicHJvZmlsZUlkIiA6ICI2OTBkMDM2OGM2NTE0OGM5ODZjMzEwN2FjMmRjNjFlYyIsCiAgInByb2ZpbGVOYW1lIiA6ICJ5emZyXzciLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDI0OGVhYTQxNGNjZjA1NmJhOTY5ZTdkODAxZmI2YTkyNzhkMGZlYWUxOGUyMTczNTZjYzhhOTQ2NTY0MzU1ZiIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9")
                    .setName(Component.literal("Battery"))
                    .glow()
            )
            gui.setSlot(
                6, GuiElementBuilder(Items.PLAYER_HEAD)
                    .setProfile(UUID.fromString("5efbb976-f210-4f78-9141-9598241a37a0"))
                    .hideDefaultTooltip()
                    .setName(Component.literal("# Alice's Head #"))
                    .glow()
            )
            gui.setSlot(
                7, GuiElementBuilder()
                    .setItem(Items.BARRIER)
                    .glow()
                    .setName(Component.literal("Bye").setStyle(Style.EMPTY.withItalic(false).withBold(true)))
                    .addLoreLine(Component.literal("Some lore"))
                    .addLoreLine(Component.literal("More lore").withStyle(ChatFormatting.RED))
                    .hideTooltip()
                    .setCount(3)
                    .setCallback(Runnable { gui.close() })
            )
            gui.setSlot(
                8, GuiElementBuilder()
                    .setItem(Items.TNT)
                    .hideDefaultTooltip()
                    .glow()
                    .setName(Component.literal("Test :)").setStyle(Style.EMPTY.withItalic(false).withBold(true)))
                    .addLoreLine(Component.literal("Some lore"))
                    .addLoreLine(Component.literal("More lore").withStyle(ChatFormatting.RED))
                    .setCount(1)
                    .setCallback { index, clickType, actionType, s ->
                        player?.sendSystemMessage(Component.literal("me when the click"), false)
                        val item = gui.getGuiElement(index)?.itemStack
                        if (clickType == ClickType.MOUSE_LEFT) {
                            item?.count = if (item.count == 1) item.count else item.count - 1
                        } else if (clickType == ClickType.MOUSE_RIGHT) {
                            item?.count += 1
                        }
                        (gui.getGuiElement(index) as SimpleGuiElement).itemStack = item
                        item?.let {
                            if (player != null) {
                                if (it.count <= player.enderChestInventory.containerSize) {
                                    gui.setSlot(
                                        4, Slot(
                                            player.enderChestInventory,
                                            item.count - it.count - 1, 0, 0
                                        )
                                    )
                                }
                            }
                        }
                    }
            )
            gui.setSlot(4, Slot(player!!.enderChestInventory, 0, 0, 0))

            gui.open()
        } catch (e: Exception) {
            LOGGER.error(e.stackTraceToString())
            e.printStackTrace()
        }
        return 0
    }
    fun testSimpleGui4(objectCommandContext: CommandContext<CommandSourceStack>): Int {
        try {
            LOGGER.info("Testing simple gui 4")
            val player = objectCommandContext.source.player

            val gui = object: SimpleGui(MenuType.GENERIC_3x3, player, true) {
                override fun onManualClose() {
                    super.onManualClose()

                    val gui = SimpleGui(MenuType.GENERIC_9x1, player, true)
                    gui.title = Component.literal("If you can take it, it's broken")
                    gui.setSlot(0, GuiElementBuilder(Items.DIAMOND, 5))
                    gui.open()
                }
            }

            gui.setSlot(0, GuiElementBuilder(Items.BARRIER, 8)
                .setCallback(Runnable { gui.close() }))
            gui.setSlot(2, GuiElementBuilder(Items.IRON_AXE).hideDefaultTooltip())
            gui.setSlot(6, GuiElementBuilder(Items.BARRIER, 9)
                .setCallback(Runnable { gui.onManualClose() }))

            gui.title = Component.literal("Close gui to test switching")
            gui.open()
        } catch (e: Exception) {
            LOGGER.error(e.stackTraceToString())
            e.printStackTrace()
        }
        return 0
    }
}