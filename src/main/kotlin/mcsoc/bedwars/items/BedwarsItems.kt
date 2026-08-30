package mcsoc.bedwars.items

import mcsoc.bedwars.utils.addItemLore
import mcsoc.bedwars.utils.applyTag
import mcsoc.bedwars.utils.renameItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

const val BEDWARS_ITEM_TAG = "bedwars_item"
const val CUSTOM_ITEM_TAG = "bedwars_custom_item"

object BedwarsItems {
    fun fireballItemStack(): ItemStack {
        val stack = Items.FIRE_CHARGE.defaultInstance
        applyTag(stack, BEDWARS_ITEM_TAG, CUSTOM_FIREBALL_VAL)
        applyTag(stack, CUSTOM_ITEM_TAG, CUSTOM_FIREBALL_VAL)
        renameItem(stack, "Fireball")
        addItemLore(stack, "Right click to shoot a fireball in the direction you look.")
        return stack
    }

    fun bridgeEggStack(): ItemStack {
        val stack = Items.EGG.defaultInstance
        applyTag(stack, BEDWARS_ITEM_TAG, CUSTOM_BRIDGE_EGG_VAL)
        applyTag(stack, CUSTOM_ITEM_TAG, CUSTOM_BRIDGE_EGG_VAL)
        renameItem(stack, "Bridge Egg")
        addItemLore(stack, "Right click to throw the egg, creating a bridge in it's wake.")
        return stack
    }
}