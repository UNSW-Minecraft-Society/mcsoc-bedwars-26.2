package mcsoc.bedwars.eventhandlers

import mcsoc.bedwars.datatrackers.ModDataTracker
import mcsoc.bedwars.datatrackers.gameState
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents


object AfterRespawnEvent {
    fun registerEvent() {
        ServerPlayerEvents.AFTER_RESPAWN.register { oldPlayer, newPlayer, alive ->
            newPlayer.level().gameState.downgradeItems(newPlayer)
        }
    }
}