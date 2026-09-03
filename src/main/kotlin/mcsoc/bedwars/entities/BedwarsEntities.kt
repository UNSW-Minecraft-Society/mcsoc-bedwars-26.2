package mcsoc.bedwars.entities

import mcsoc.bedwars.datatrackers.customEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.npc.villager.Villager
import net.minecraft.world.phys.Vec3

const val BEDWARS_ENTITY_TAG = "bedwars_entity"
const val CUSTOM_SHOPKEEPER_VAL = "shopkeeper"

fun spawnShopkeeper(level: ServerLevel, position: Vec3, type: CustomEntityType) {
    val shopkeeper = Villager(EntityTypes.VILLAGER, level)
    shopkeeper.setPos(position)
    shopkeeper.isNoAi = false
    shopkeeper.isInvulnerable = true
    level.customEntityData[shopkeeper.uuid] = type
    level.addFreshEntity(shopkeeper)
}