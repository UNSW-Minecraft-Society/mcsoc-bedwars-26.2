package mcsoc.bedwars.utils

import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.item.component.ItemLore

fun applyTag(stack: ItemStack, key: String, value: String): ItemStack {
    val tag = CompoundTag()
    tag.putString(key, value)
    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag))
    return stack
}

fun renameItem(stack: ItemStack, name: String, vararg formats: ChatFormatting): ItemStack {
    val value = Component.literal(name).withStyle(*formats)
    stack.set(DataComponents.ITEM_NAME, value)
    return stack
}

fun renameItem(stack: ItemStack, name: String): ItemStack = renameItem(
    stack, name, ChatFormatting.WHITE, ChatFormatting.RESET
)

fun addItemLore(stack: ItemStack, value: Component): ItemStack {
    val lore = stack.get(DataComponents.LORE)
    if (lore != null)
        stack.set(DataComponents.LORE, lore.withLineAdded(value))
    else
        stack.set(DataComponents.LORE, ItemLore(listOf(value)))
    return stack
}

fun addItemLore(stack: ItemStack, description: String): ItemStack = addItemLore(
    stack, Component.literal(description)
)
