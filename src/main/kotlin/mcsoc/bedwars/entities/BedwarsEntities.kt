package mcsoc.bedwars.entities

import mcsoc.bedwars.BedwarsPlugin
import mcsoc.bedwars.datatrackers.CustomEntityType
import mcsoc.bedwars.datatrackers.customEntityData
import mcsoc.bedwars.datatrackers.eventQueue
import mcsoc.bedwars.datatrackers.generatorstate.InvalidTeamException
import mcsoc.bedwars.utils.Team
import mcsoc.bedwars.utils.ticks
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.animal.golem.IronGolem
import net.minecraft.world.entity.npc.villager.Villager
import net.minecraft.world.phys.Vec3
import kotlin.time.Duration

fun spawnShopkeeper(level: ServerLevel, position: Vec3, type: CustomEntityType) {
    val shopkeeper = Villager(EntityTypes.VILLAGER, level)
    shopkeeper.setPos(position)
    shopkeeper.isNoAi = true
    shopkeeper.isInvulnerable = true
    shopkeeper.customName = Component.literal(type.title)
    shopkeeper.isCustomNameVisible = true
    level.customEntityData.addEntity(shopkeeper, type)
    level.addFreshEntity(shopkeeper)
}