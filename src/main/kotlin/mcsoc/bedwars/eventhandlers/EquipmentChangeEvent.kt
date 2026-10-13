package mcsoc.bedwars.eventhandlers

import mcsoc.bedwars.datatrackers.timeState
import mcsoc.bedwars.datatrackers.timetracker.GamePeriod
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemStack


private fun armourUnequiped(equipmentSlot: EquipmentSlot, previous: ItemStack, next: ItemStack): Boolean = equipmentSlot.isArmor && !previous.isEmpty && next.isEmpty

fun registerEquipmentChangeEvents() {
    ServerEntityEvents.EQUIPMENT_CHANGE.register { player, equipmentSlot, previous, next ->
        // This could be cleaner as a mixin but I'm not bothered rn
        if (player is ServerPlayer && player.timeState.getGamePeriod() != GamePeriod.INACTIVE) {
            if (armourUnequiped(equipmentSlot, previous, next)) player.setItemSlot(equipmentSlot, previous)
        }
    }
}
