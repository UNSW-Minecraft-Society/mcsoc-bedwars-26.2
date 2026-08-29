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
}