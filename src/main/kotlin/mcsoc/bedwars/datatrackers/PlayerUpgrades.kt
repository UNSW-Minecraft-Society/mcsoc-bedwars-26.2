package mcsoc.bedwars.datatrackers

import net.minecraft.core.registries.Registries
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.enchantment.Enchantments

enum class ToolType {
    PICKAXE {
        override fun getItem(tier: Int, level: ServerLevel): ItemStack? {
            val item = when (tier) {
                1 -> EnchantedItem(ItemStack(Items.WOODEN_PICKAXE), 1)
                2 -> EnchantedItem(ItemStack(Items.IRON_PICKAXE), 2)
                3 -> EnchantedItem(ItemStack(Items.GOLDEN_PICKAXE), 3)
                4 -> EnchantedItem(ItemStack(Items.DIAMOND_PICKAXE), 3)
                else -> return null
            }
            val ench = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.EFFICIENCY)
            item.item.enchant(ench, item.efficiency)
            return item.item
        }
    },
    AXE {
        override fun getItem(tier: Int, level: ServerLevel): ItemStack? {
            val item = when (tier) {
                1 -> EnchantedItem(ItemStack(Items.WOODEN_AXE), 1)
                2 -> EnchantedItem(ItemStack(Items.STONE_AXE), 1)
                3 -> EnchantedItem(ItemStack(Items.IRON_AXE), 2)
                4 -> EnchantedItem(ItemStack(Items.DIAMOND_AXE), 3)
                else -> return null
            }
            val ench = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.EFFICIENCY)
            item.item.enchant(ench, item.efficiency)
            return item.item
        }
    };

    abstract fun getItem(tier: Int, level: ServerLevel): ItemStack?
}

private data class EnchantedItem(val item: ItemStack, val efficiency: Int)

internal interface PlayerUpgradesRecord {
    fun upgradeTool(tool: ToolType)
    fun ressetToolUpgrade(tool: ToolType)
    fun getCurrentUpgrade(tool: ToolType): Int
}

internal interface PlayerUpgradesExposer {
    fun getTool(player: ServerPlayer, tool: ToolType): ItemStack?
}

internal interface PlayerUpgradesHolder : PlayerUpgradesExposer {
    fun getToolUpgradeState(player: ServerPlayer): PlayerUpgradesRecord

    override fun getTool(player: ServerPlayer, tool: ToolType): ItemStack? {
        val level = getToolUpgradeState(player).getCurrentUpgrade(tool)
        return tool.getItem(level, player.level())
    }
}
