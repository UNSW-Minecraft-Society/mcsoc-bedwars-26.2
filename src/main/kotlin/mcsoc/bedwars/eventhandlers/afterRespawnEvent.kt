package mcsoc.bedwars.eventhandlers

import mcsoc.bedwars.datatrackers.ModDataTracker
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents


object AfterRespawnEvent {
    fun registerEvent() {
        ServerPlayerEvents.AFTER_RESPAWN.register { oldPlayer, newPlayer, alive ->
            ModDataTracker.downgradeTools(newPlayer)
        }
    }
}