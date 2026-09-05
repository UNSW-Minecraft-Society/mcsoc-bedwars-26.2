package mcsoc.bedwars.eventhandlers

import mcsoc.bedwars.datatrackers.ModDataTracker
import mcsoc.bedwars.gamestate.GameManager
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents

fun registerEventHandlers() {
    ServerTickEvents.END_LEVEL_TICK.register{ level ->
        GameManager.tick(level)
    }
    registerItemCallbacks()
    registerEntityCallbacks()
    registerPlayerJoinEvent()
}