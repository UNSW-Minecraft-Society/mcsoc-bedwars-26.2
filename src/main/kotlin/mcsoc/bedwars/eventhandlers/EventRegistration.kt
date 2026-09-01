package mcsoc.bedwars.eventhandlers

import mcsoc.bedwars.datatrackers.ModDataTracker
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents

fun registerEventHandlers() {
    ServerTickEvents.END_SERVER_TICK.register{server ->
        ModDataTracker.tick()
    }
    registerItemCallbacks()
    registerPlayerJoinEvent()
}
