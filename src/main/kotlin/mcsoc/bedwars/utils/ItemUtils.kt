package mcsoc.bedwars.utils

import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.component.ItemLore
import net.minecraft.world.item.enchantment.Enchantment


fun ItemStack.withTag(key: String, value: String): ItemStack {
    val tag = CompoundTag()
    tag.putString(key, value)
    this.set(DataComponents.CUSTOM_DATA, CustomData.of(tag))
    return this
}

fun ItemStack.hasTag(key: String, value: String): Boolean {
    val data = this.get(DataComponents.CUSTOM_DATA) ?: return false
    return data.copyTag().getString(key).orElse(null) == value
}

fun ItemStack.renamedTo(name: String, vararg formats: ChatFormatting): ItemStack {
    val value = Component.literal(name).withStyle(*formats)
    this.set(DataComponents.ITEM_NAME, value)
    return this
}

fun ItemStack.renamedTo(name: String): ItemStack = this.renamedTo(
    name, ChatFormatting.WHITE, ChatFormatting.RESET
)

fun ItemStack.withItemLore(value: Component): ItemStack {
    val lore = this.get(DataComponents.LORE)
    if (lore != null)
        this.set(DataComponents.LORE, lore.withLineAdded(value))
    else
        this.set(DataComponents.LORE, ItemLore(listOf(value)))
    return this
}

fun ItemStack.withItemLore(description: String): ItemStack = this.withItemLore(
    Component.literal(description)
)

fun ItemStack.withEnchant(ench: ResourceKey<Enchantment>, enchLevel: Int, level: ServerLevel): ItemStack {
    if (enchLevel >= 0) {
        val ench = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(ench)
        this.enchant(ench, enchLevel)
    }
    return this
}