package mcsoc.bedwars.items

import mcsoc.bedwars.utils.applyTag
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.CustomData

const val BEDWARS_ITEM_TAG = "bedwars_item"
const val CUSTOM_ITEM_TAG = "bedwars_custom_item"

object BedwarsItems {
    fun fireballItemStack(): ItemStack {
        var stack = Items.FIRE_CHARGE.defaultInstance
        applyTag(stack, BEDWARS_ITEM_TAG, CUSTOM_FIREBALL_VAL)
        applyTag(stack, CUSTOM_ITEM_TAG, CUSTOM_FIREBALL_VAL)
        return stack
    }
}