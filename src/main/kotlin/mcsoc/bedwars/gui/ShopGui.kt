package mcsoc.bedwars.gui

import eu.pb4.sgui.api.ClickType
import eu.pb4.sgui.api.elements.GuiElement
import eu.pb4.sgui.api.elements.GuiElementBuilder
import eu.pb4.sgui.api.gui.SimpleGui
import mcsoc.bedwars.BedwarsPlugin
import mcsoc.bedwars.datatrackers.generatorstate.InvalidTeamException
import mcsoc.bedwars.datatrackers.shopConfig
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

/**
 * Object containing logic pertaining to displaying (via an inventory GUI) items in the shop.
 */
object ShopGui {
    /**
     * Displays the shop GUI to the provided player. Returns 1 if succeeded.
     */
    fun displayShop(player: ServerPlayer, shopType: ShopType) {
        try {
            /**
             * Update items within the shop gui.
             */
            fun updateItems(gui: SimpleGui) {
                val shopConfig = gui.player.level().shopConfig
                val products = ShopInventory.getProducts(shopType, shopConfig)
                for ((slotIndex, product) in ShopInventory.getProductSlotIndex(shopType, shopConfig) zip products) {
                    if (product is PlayerSpecificShopProduct) product.setShopPlayer(player)
                    val element = GuiElementBuilder(product.getItemStack())
                        .setCallback(product.getClickCallback())
                    element.setName(product.getProductName())
                    for (line in product.getDescriptionLines()) element.addLoreLine(line)
                    element.addLoreLine(Component.literal("Cost: ${product.getItemCost()?.count} ").append(product.getItemCost()?.hoverName ?: Component.empty()))
                    gui.setSlot(slotIndex, element)
                }
            }

            val gui = object : SimpleGui(ShopInventory.getShopMenuType(shopType, player.level().shopConfig), player, false) {
                override fun onClick(
                    index: Int,
                    type: ClickType?,
                    action: ContainerInput?,
                    element: GuiElement?
                ): Boolean {
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