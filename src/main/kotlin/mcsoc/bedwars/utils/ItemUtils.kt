package mcsoc.bedwars.utils

import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData

fun applyTag(stack: ItemStack, key: String, value: String): ItemStack {
    val tag = CompoundTag()
    tag.putString(key, value)
    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag))
    return stack
}
