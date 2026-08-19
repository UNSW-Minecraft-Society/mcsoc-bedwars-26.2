package mcsoc.bedwars.datatrackers

import mcsoc.bedwars.upgrades.Resettable
import mcsoc.bedwars.upgrades.Single
import mcsoc.bedwars.upgrades.UpgradableItem
import mcsoc.bedwars.upgrades.UpgradeItemType
import net.minecraft.core.component.DataComponents
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack


internal interface PlayerUpgradesRecord {
    fun getItem(item: UpgradeItemType): UpgradableItem
    fun setItem(item: UpgradableItem)
    fun removeItem(item: UpgradeItemType)
}

internal interface PlayerUpgradesExposer {
    fun upgradeItem(player: ServerPlayer, item: UpgradeItemType)
    fun downgradeItems(player: ServerPlayer)
    fun clearItems(player: ServerPlayer)
    fun getItemStack(player: ServerPlayer, item: UpgradeItemType): ItemStack
}

internal interface PlayerUpgradesHolder : PlayerUpgradesExposer {
    fun getItemUpgradeState(player: Player): PlayerUpgradesRecord

    override fun upgradeItem(player: ServerPlayer, item: UpgradeItemType) {
        val upgrade = getItemUpgradeState(player).getItem(item).next() ?: return
        getItemUpgradeState(player).setItem(upgrade)
        upgrade.applyTo(player)
    }

    override fun downgradeItems(player: ServerPlayer) {
        val record = getItemUpgradeState(player)
        UpgradeItemType.entries.forEach { type ->
            val prev = record.getItem(type).prev()
            record.setItem(prev)
            prev.applyTo(player)
        }
    }

    override fun clearItems(player: ServerPlayer) {
        val record = getItemUpgradeState(player)
        UpgradeItemType.entries.forEach {
            record.removeItem(it)
        }
    }

    override fun getItemStack(player: ServerPlayer, item: UpgradeItemType): ItemStack {
        val item_type = getItemUpgradeState(player).getItem(item)
        if (item_type is Single) { // TEMPORARY THINGY, MODIFY LATER
            return item_type.createStack(player)
        }
        TODO("Not yet implemented; waiting for PlayerUpgrade changes")
    }
}
