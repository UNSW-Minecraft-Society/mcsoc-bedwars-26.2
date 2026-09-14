package mcsoc.bedwars.entities

import mcsoc.bedwars.BedwarsPlugin
import mcsoc.bedwars.datatrackers.CustomEntityType
import mcsoc.bedwars.datatrackers.customEntityData
import mcsoc.bedwars.datatrackers.eventQueue
import mcsoc.bedwars.datatrackers.generatorstate.InvalidTeamException
import mcsoc.bedwars.utils.Team
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.animal.golem.IronGolem
import net.minecraft.world.entity.npc.villager.Villager
import net.minecraft.world.phys.Vec3


const val GOLEM_EXPIRY_TIME_TICKS: Long = 400

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

fun spawnDoomedDefender(level: ServerLevel, pos: Vec3, team: Team) {
    val scoreboardTeam = level.scoreboard.getPlayerTeam(team.getName()) ?: run {
        BedwarsPlugin.LOGGER.error("spawnDoomedDefender: ", InvalidTeamException(team))
        return
    }
    
    val golem = IronGolem(EntityTypes.IRON_GOLEM, level)
    golem.setPos(pos)
    if (level.addFreshEntity(golem)) {
        level.eventQueue.queueEntityExpiry(GOLEM_EXPIRY_TIME_TICKS, golem.uuid)
        level.scoreboard.addPlayerToTeam(golem.stringUUID, scoreboardTeam)
    }
}