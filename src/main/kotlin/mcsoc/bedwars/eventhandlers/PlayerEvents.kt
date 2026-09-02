package mcsoc.bedwars.eventhandlers

import mcsoc.bedwars.datatrackers.getModData
import mcsoc.bedwars.gamestate.GameManager
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.minecraft.server.level.ServerPlayer


fun AfterDeathEvent() {
    ServerLivingEntityEvents.AFTER_DEATH.register{maybe_player, death_source ->
        if (maybe_player is ServerPlayer) GameManager.handlePlayerDeath(maybe_player, death_source)
    }
}


fun AfterRespawnEvent() {
    ServerPlayerEvents.AFTER_RESPAWN.register { oldPlayer, newPlayer, alive ->
        GameManager.handlePlayerRespawn(newPlayer)
    }
}


fun registerPlayerJoinEvent() {
//    removed in favour of adding active players on game start
//    ServerPlayerEvents.JOIN.register { player ->
//        ModDataTracker.addActivePlayer(player.uuid)
//    }
}