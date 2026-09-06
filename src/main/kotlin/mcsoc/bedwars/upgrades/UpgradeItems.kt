package mcsoc.bedwars.upgrades

import com.mojang.serialization.Codec
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.enchantment.Enchantments


enum class UpgradeItemType(val defaultStr: String, val fromName: (String) -> UpgradableItem) {
    AXE("NONE", Axe::valueOf),
    PICKAXE("NONE", Pickaxe::valueOf),
    SWORD("WOODEN", Sword::valueOf),
    ARMOUR("LEATHER", Armour::valueOf);

    val default: UpgradableItem
        get() = fromName(defaultStr)

    companion object {
        val CODEC: Codec<UpgradeItemType> = Codec.STRING.xmap(::valueOf, UpgradeItemType::name)
    }
}

interface UpgradableItem {
    val type: UpgradeItemType

    fun next(): UpgradableItem?
    fun prev(): UpgradableItem = this
    fun tier(): Int

    fun applyTo(player: ServerPlayer)

    // For displaying upgrade as an item
    fun createStack(player: ServerPlayer): ItemStack

    fun isItemThisUpgrade(item: ItemStack): Boolean {
        val data = item.get(DataComponents.CUSTOM_DATA) ?: return false

        return data.copyTag().getString("bedwars_item").orElse(null) == type.name
    }
}

internal interface Single : UpgradableItem {
    val material: Item

    override fun createStack(player: ServerPlayer): ItemStack {
        val stack = ItemStack(material)
        val tag = CompoundTag()
        tag.putString("bedwars_item", type.name)
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag))
        return stack
    }

    override fun applyTo(player: ServerPlayer) {
        val stack = createStack(player)
        for (slot in 0 until player.inventory.containerSize) {
            if (isItemThisUpgrade(player.inventory.getItem(slot))) {
                player.inventory.setItem(slot, stack)
                return
            }
        }

        player.inventory.add(stack)
    }
}

internal interface EnchantableItem : Single {
    val efficiency: Int

    override fun createStack(player: ServerPlayer): ItemStack {
        val stack = super.createStack(player)
        if (efficiency > 0) {
            val ench = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.EFFICIENCY)
            stack.enchant(ench, efficiency)
        }
        return stack
    }
}

internal interface Resettable : UpgradableItem {
    fun base(): UpgradableItem
    override fun prev() = base()
}


enum class Pickaxe(override val material: Item, override val efficiency: Int) : EnchantableItem {
    NONE(Items.AIR, 0) {
        override fun next() = WOODEN
        override fun prev() = NONE
        override fun tier() = 0
    },
    WOODEN(Items.WOODEN_PICKAXE, 1) {
        override fun next() = IRON
        override fun prev() = WOODEN
        override fun tier() = 1
    },
    IRON(Items.IRON_PICKAXE, 2) {
        override fun next() = GOLDEN
        override fun prev() = WOODEN
        override fun tier() = 2
    },
    GOLDEN(Items.GOLDEN_PICKAXE, 3) {
        override fun next() = DIAMOND
        override fun prev() = IRON
        override fun tier() = 2
    },
    DIAMOND(Items.DIAMOND_PICKAXE, 3) {
        override fun next() = null
        override fun prev() = GOLDEN
        override fun tier() = 3
    };

    override val type = UpgradeItemType.PICKAXE
}

enum class Axe(override val material: Item, override val efficiency: Int) : EnchantableItem {
    NONE(Items.AIR, 0) {
        override fun next() = WOODEN
        override fun prev() = NONE
        override fun tier() = 0
    },
    WOODEN(Items.WOODEN_AXE, 1) {
        override fun next() = STONE
        override fun prev() = WOODEN
        override fun tier() = 1
    },
    STONE(Items.STONE_AXE, 1) {
        override fun next() = IRON
        override fun prev() = WOODEN
        override fun tier() = 2
    },
    IRON(Items.IRON_AXE, 2) {
        override fun next() = DIAMOND
        override fun prev() = STONE
        override fun tier() = 3
    },
    DIAMOND(Items.DIAMOND_AXE, 3) {
        override fun next() = null
        override fun prev() = IRON
        override fun tier() = 4
    };

    override val type = UpgradeItemType.AXE
}

enum class Sword(override val material: Item) : Single, Resettable {
    WOODEN(Items.WOODEN_SWORD) {
        override fun next() = STONE
        override fun tier() = 0

    },
    STONE(Items.STONE_SWORD) {
        override fun next() = IRON
        override fun tier() = 1

    },
    IRON(Items.IRON_SWORD) {
        override fun next() = DIAMOND
        override fun tier() = 2

    },
    DIAMOND(Items.DIAMOND_SWORD) {
        override fun next() = null
        override fun tier() = 3

    };

    override fun base() = WOODEN
    override val type = UpgradeItemType.SWORD
}

enum class Armour(val boots: Item, val leggings: Item, val chestplate: Item) : UpgradableItem {
    LEATHER(Items.LEATHER_BOOTS, Items.LEATHER_LEGGINGS, Items.LEATHER_CHESTPLATE) {
        override fun next() = CHAINMAIL
        override fun tier() = 0

    },
    CHAINMAIL(Items.CHAINMAIL_BOOTS, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_CHESTPLATE) {
        override fun next() = IRON
        override fun tier() = 1

    },
    IRON(Items.IRON_BOOTS, Items.IRON_LEGGINGS, Items.IRON_CHESTPLATE) {
        override fun next() = DIAMOND
        override fun tier() = 2

    },
    DIAMOND(Items.DIAMOND_BOOTS, Items.DIAMOND_LEGGINGS, Items.DIAMOND_CHESTPLATE) {
        override fun next() = null
        override fun tier() = 3
    };

    override val type = UpgradeItemType.ARMOUR

    private fun setTo(player: ServerPlayer, slot: EquipmentSlot, material: Item) {
        val stack = ItemStack(material)
        val tag = CompoundTag()
        tag.putString("bedwars_item", type.name)
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag))
        player.setItemSlot(slot, stack)
    }

    override fun applyTo(player: ServerPlayer) {
        setTo(player, EquipmentSlot.FEET, boots)
        setTo(player, EquipmentSlot.LEGS, leggings)
        setTo(player, EquipmentSlot.CHEST, chestplate)
    }

    override fun createStack(player: ServerPlayer): ItemStack {
        return ItemStack(chestplate)
    }
}
