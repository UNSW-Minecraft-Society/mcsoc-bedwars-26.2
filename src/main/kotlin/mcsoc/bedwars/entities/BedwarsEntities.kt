package mcsoc.bedwars.entities

import mcsoc.bedwars.datatrackers.CustomEntityType
import mcsoc.bedwars.datatrackers.customEntityData
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.npc.villager.Villager
import net.minecraft.world.phys.Vec3

private fun createShopkeeper(level: ServerLevel, position: Vec3, title: Component): Entity {
    return Villager(EntityTypes.VILLAGER, level)
        // set Shopkeeper Entity attributes
        .also{
            it.setPos(position) 
            it.isNoAi = true
            it.isInvulnerable = true
            it.isCustomNameVisible = true
            it.customName = title
        }
}

fun placeShopkeeper(level: ServerLevel, shop: Entity, type: CustomEntityType) {
    level.customEntityData.addEntity(shop, type) 
    level.addFreshEntity(shop)
}

fun spawnShopkeeper(level: ServerLevel, position: Vec3, type: CustomEntityType, also: (Entity) -> Unit = {}) {
    createShopkeeper(level, position, Component.literal(type.title))
        .also(also)
        .also{ placeShopkeeper(level, it, type) }
}