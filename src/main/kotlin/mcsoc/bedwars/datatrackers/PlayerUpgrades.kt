package mcsoc.bedwars.datatrackers

import com.mojang.serialization.Codec
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.enchantment.Enchantments

// stores the base tool
enum class ToolCategory(val firstTier: ToolTier) {
    PICKAXE(ToolTier.Pickaxe.WOODEN),
    AXE(ToolTier.Axe.WOODEN);

    companion object {
        val CODEC = Codec.STRING.xmap(ToolCategory::valueOf, ToolCategory::name)
    }
}

sealed interface ToolTier {
    val material: Item
    val efficiencyLevel: Int
    val category: ToolCategory

    fun createItem(level: ServerLevel): ItemStack {
        val ench = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.EFFICIENCY)
        val stack = ItemStack(material)
        stack.enchant(ench, efficiencyLevel)
        stack.set(
            DataComponents.CUSTOM_DATA,
            CustomData.of(CompoundTag().apply {
                putBoolean("bedwars", true); putString(
                "tool_category", category.name
            )
            }),
        )
        return stack
    }

    companion object {
        val CODEC: Codec<ToolTier> =
            Codec.STRING.xmap(
                { str ->
                    val (category, tier) = str.split(":")
                    when (ToolCategory.valueOf(category)) {
                        ToolCategory.PICKAXE -> ToolTier.Pickaxe.valueOf(tier)
                        ToolCategory.AXE -> ToolTier.Axe.valueOf(tier)
                    }
                },
                { tier ->
                    "${tier}:${(tier as Enum<*>).name}"
                }
            )
    }

    enum class Pickaxe(override val material: Item, override val efficiencyLevel: Int) : ToolTier {
        WOODEN(Items.WOODEN_PICKAXE, 1) {
            override fun next(): ToolTier = IRON
        },
        IRON(Items.IRON_PICKAXE, 2) {
            override fun next(): ToolTier = GOLDEN
        },
        GOLDEN(Items.GOLDEN_PICKAXE, 3) {
            override fun next(): ToolTier = DIAMOND
        },
        DIAMOND(Items.DIAMOND_PICKAXE, 3) {
            override fun next(): ToolTier = DIAMOND
        };

        override val category: ToolCategory
            get() = ToolCategory.PICKAXE
    }

    enum class Axe(override val material: Item, override val efficiencyLevel: Int) : ToolTier {
        WOODEN(Items.WOODEN_AXE, 1) {
            override fun next(): ToolTier = STONE
        },
        STONE(Items.STONE_AXE, 1) {
            override fun next(): ToolTier = IRON
        },
        IRON(Items.IRON_AXE, 2) {
            override fun next(): ToolTier = DIAMOND
        },
        DIAMOND(Items.DIAMOND_AXE, 3) {
            override fun next(): ToolTier = DIAMOND
        };

        override val category: ToolCategory
            get() = ToolCategory.AXE
    }

    fun next(): ToolTier
}

internal interface PlayerUpgradesRecord {
    fun getTool(tool: ToolCategory): ToolTier?
    fun setTool(tool: ToolCategory, tier: ToolTier?)
}

internal interface PlayerUpgradesExposer {
    fun getTool(player: ServerPlayer, tool: ToolCategory): ItemStack?
    fun upgradeTool(player: ServerPlayer, tool: ToolCategory)
    fun downgradeTools(player: ServerPlayer)
    fun clearTools(player: ServerPlayer)
}

internal interface PlayerUpgradesHolder : PlayerUpgradesExposer {
    fun getToolUpgradeState(player: ServerPlayer): PlayerUpgradesRecord

    override fun getTool(player: ServerPlayer, tool: ToolCategory): ItemStack? {
        return getToolUpgradeState(player).getTool(tool)?.createItem(player.level())
    }

    override fun upgradeTool(player: ServerPlayer, tool: ToolCategory) {
        val upgrade = getToolUpgradeState(player).getTool(tool)?.next() ?: tool.firstTier
        getToolUpgradeState(player).setTool(tool, upgrade)

        removeTool(player, tool)
        player.inventory.add(upgrade.createItem(player.level()))
    }

    override fun downgradeTools(player: ServerPlayer) {
        val record = getToolUpgradeState(player)
        ToolCategory.entries.forEach { tool ->
            val item = record.getTool(tool)
            if (item != null) {
                record.setTool(tool, tool.firstTier)
                removeTool(player, tool)
                player.inventory.add(tool.firstTier.createItem(player.level()))
            }
        }
    }

    override fun clearTools(player: ServerPlayer) {
        val record = getToolUpgradeState(player)
        ToolCategory.entries.forEach {
            record.setTool(it, null)
            removeTool(player, it)
        }
    }

}


private fun isBedwarsTool(stack: ItemStack, type: ToolCategory): Boolean {
    val data = stack.get(DataComponents.CUSTOM_DATA) ?: return false
    val tag = data.copyTag()

    return tag.getBoolean("bedwars").get()
            && tag.getString("tool_category").get() == type.name
}

fun removeTool(player: ServerPlayer, category: ToolCategory) {
    for (slot in 0 until player.inventory.containerSize) {
        val stack = player.inventory.getItem(slot)

        if (isBedwarsTool(stack, category)) {
            player.inventory.setItem(slot, ItemStack.EMPTY)
            return
        }
    }
}