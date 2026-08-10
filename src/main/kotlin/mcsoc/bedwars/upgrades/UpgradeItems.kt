package mcsoc.bedwars.upgrades

import com.mojang.serialization.Codec
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.level.Level


enum class UpgradeItemType(val defaultStr: String, val fromName: (String) -> UpgradableItem) {
    AXE("NONE", Axe::valueOf),
    PICKAXE("NONE", Pickaxe::valueOf),
    SWORD("WOODEN", Sword::valueOf),
    ARMOUR_BOOTS("LEATHER", ArmourBoots::valueOf),
    ARMOUR_LEGGINGS("LEATHER", ArmourLeggings::valueOf),
    ARMOUR_CHESTPLATE("LEATHER", ArmourChestplate::valueOf);
    
    val default: UpgradableItem 
        get() = fromName(defaultStr)

    companion object {
        val CODEC: Codec<UpgradeItemType> = Codec.STRING.xmap(::valueOf, UpgradeItemType::name)
    }
}

sealed interface UpgradableItem {
    val material: Item
    val type: UpgradeItemType

    fun next(): UpgradableItem?

    fun createItem(level: Level): ItemStack {
        val stack = ItemStack(material)
        val tag = CompoundTag()
        tag.putString("bedwars_item", type.name)
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag))
        return stack
    }
}

sealed interface Resettable {
    fun base(): UpgradableItem
}

sealed interface Downgradable {
    fun prev(): UpgradableItem
}

sealed interface EnchantableItem : UpgradableItem {
    val efficiency: Int

    override fun createItem(level: Level): ItemStack {
        val item = super.createItem(level)
        if (efficiency > 0) {
            val ench = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.EFFICIENCY)
            item.enchant(ench, efficiency)
        }
        return item
    }
}

enum class Pickaxe(override val material: Item, override val efficiency: Int) : EnchantableItem, Downgradable {
    NONE(Items.AIR, 0) {
        override fun next() = WOODEN
        override fun prev() = NONE
    },
    WOODEN(Items.WOODEN_PICKAXE, 1) {
        override fun next() = IRON
        override fun prev() = WOODEN
    },
    IRON(Items.IRON_PICKAXE, 2) {
        override fun next() = GOLDEN
        override fun prev() = WOODEN
    },
    GOLDEN(Items.GOLDEN_PICKAXE, 3) {
        override fun next() = DIAMOND
        override fun prev() = IRON
    },
    DIAMOND(Items.DIAMOND_PICKAXE, 3) {
        override fun next() = null
        override fun prev() = GOLDEN
    };

    override val type = UpgradeItemType.PICKAXE
}

enum class Axe(override val material: Item, override val efficiency: Int) : EnchantableItem, Downgradable {
    NONE(Items.AIR, 0) {
        override fun next() = WOODEN
        override fun prev() = NONE
    },
    WOODEN(Items.WOODEN_AXE, 1) {
        override fun next() = STONE
        override fun prev() = WOODEN
    },
    STONE(Items.STONE_AXE, 1) {
        override fun next() = IRON
        override fun prev() = WOODEN
    },
    IRON(Items.IRON_AXE, 2) {
        override fun next() = DIAMOND
        override fun prev() = STONE
    },
    DIAMOND(Items.DIAMOND_AXE, 3) {
        override fun next() = null
        override fun prev() = IRON
    };

    override val type = UpgradeItemType.AXE
}

enum class Sword(override val material: Item) : UpgradableItem, Resettable {
    WOODEN(Items.WOODEN_SWORD) { override fun next() = STONE },
    STONE(Items.STONE_SWORD) { override fun next() = IRON },
    IRON(Items.IRON_SWORD) { override fun next() = DIAMOND },
    DIAMOND(Items.DIAMOND_SWORD) { override fun next() = null };

    override fun base() = WOODEN
    override val type = UpgradeItemType.SWORD
}

enum class ArmourBoots(override val material: Item) : UpgradableItem {
    LEATHER(Items.LEATHER_BOOTS) { override fun next() = CHAINMAIL },
    CHAINMAIL(Items.CHAINMAIL_BOOTS) { override fun next() = IRON },
    IRON(Items.IRON_BOOTS) { override fun next() = DIAMOND },
    DIAMOND(Items.DIAMOND_BOOTS) { override fun next() = null };

    override val type = UpgradeItemType.ARMOUR_BOOTS
}

enum class ArmourLeggings(override val material: Item) : UpgradableItem {
    LEATHER(Items.LEATHER_LEGGINGS) { override fun next() = CHAINMAIL },
    CHAINMAIL(Items.CHAINMAIL_LEGGINGS) { override fun next() = IRON },
    IRON(Items.IRON_LEGGINGS) { override fun next() = DIAMOND },
    DIAMOND(Items.DIAMOND_LEGGINGS) { override fun next() = null };

    override val type = UpgradeItemType.ARMOUR_LEGGINGS
}

enum class ArmourChestplate(override val material: Item) : UpgradableItem {
    LEATHER(Items.LEATHER_CHESTPLATE) { override fun next() = CHAINMAIL },
    CHAINMAIL(Items.CHAINMAIL_CHESTPLATE) { override fun next() = IRON },
    IRON(Items.IRON_CHESTPLATE) { override fun next() = DIAMOND },
    DIAMOND(Items.DIAMOND_CHESTPLATE) { override fun next() = null };

    override val type = UpgradeItemType.ARMOUR_CHESTPLATE
}
