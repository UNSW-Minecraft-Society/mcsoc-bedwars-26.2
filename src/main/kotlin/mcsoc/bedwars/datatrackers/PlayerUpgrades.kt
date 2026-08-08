package mcsoc.bedwars.datatrackers

import mcsoc.bedwars.upgrades.Downgradable
import mcsoc.bedwars.upgrades.Resettable
import mcsoc.bedwars.upgrades.UpgradableItem
import mcsoc.bedwars.upgrades.UpgradeItemType
import net.minecraft.core.component.DataComponents
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack


internal interface PlayerUpgradesRecord {
    fun getItem(item: UpgradeItemType): UpgradableItem
    fun setItem(item: UpgradableItem)
    fun removeItem(item: UpgradeItemType)
}

internal interface PlayerUpgradesExposer {
    fun getItem(player: ServerPlayer, item: UpgradeItemType): ItemStack
    fun upgradeItem(player: ServerPlayer, item: UpgradeItemType)
    fun downgradeItems(player: ServerPlayer)
    fun clearItems(player: ServerPlayer)
}

internal interface PlayerUpgradesHolder : PlayerUpgradesExposer {
    fun getItemUpgradeState(player: ServerPlayer): PlayerUpgradesRecord

    override fun getItem(player: ServerPlayer, item: UpgradeItemType): ItemStack {
        return getItemUpgradeState(player).getItem(item).createItem(player.level())
    }

    override fun upgradeItem(player: ServerPlayer, item: UpgradeItemType) {
        val upgrade = getItemUpgradeState(player).getItem(item).next() ?: return
        getItemUpgradeState(player).setItem(upgrade)

        changeItem(player, item, upgrade)
    }

    override fun downgradeItems(player: ServerPlayer) {
        val record = getItemUpgradeState(player)
        UpgradeItemType.entries.forEach { type ->
            val item = record.getItem(type)
            if (item is Resettable) {
                record.setItem(item.base())
                changeItem(player, type, item.base())
                return@forEach
            } else if (item is Downgradable) {
                val prev = item.prev() ?: return@forEach
                record.setItem(prev)
                changeItem(player, type, prev)
                return@forEach
            }
            
            changeItem(player, type, item)
        }
    }

    override fun clearItems(player: ServerPlayer) {
        val record = getItemUpgradeState(player)
        UpgradeItemType.entries.forEach {
            record.removeItem(it)
            removeItem(player, it)
        }
    }
    
    private fun changeItem(p: ServerPlayer, type: UpgradeItemType, item: UpgradableItem) {
        val slot = removeItem(p, type)
        if (slot != null) {
            p.inventory.add(slot, item.createItem(p.level()))
        } else {
            p.inventory.add(item.createItem(p.level()))
        }
    }

    private fun removeItem(player: ServerPlayer, type: UpgradeItemType): Int? {
        for (slot in 0 until player.inventory.containerSize) {
            val stack = player.inventory.getItem(slot)
            val data = stack.get(DataComponents.CUSTOM_DATA) ?: continue

            if (data.copyTag().getString("bedwars_item").orElse(null) == type.name) {
                player.inventory.setItem(slot, ItemStack.EMPTY)
                return slot
            }
        }
        return null
    }
}
