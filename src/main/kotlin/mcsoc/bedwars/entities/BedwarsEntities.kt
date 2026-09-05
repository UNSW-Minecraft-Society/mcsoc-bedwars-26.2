package mcsoc.bedwars.entities

import mcsoc.bedwars.datatrackers.customEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.npc.villager.Villager
import net.minecraft.world.phys.Vec3

fun spawnShopkeeper(level: ServerLevel, position: Vec3, type: CustomEntityType) {
    val shopkeeper = Villager(EntityTypes.VILLAGER, level)
    shopkeeper.setPos(position)
    shopkeeper.isNoAi = true
    shopkeeper.isInvulnerable = true
    level.customEntityData[shopkeeper.uuid] = type
    level.addFreshEntity(shopkeeper)
}