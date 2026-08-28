package mcsoc.bedwars.eventhandlers

import mcsoc.bedwars.datatrackers.ModDataTracker
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents

fun registerServerTick() {
    ServerTickEvents.END_SERVER_TICK.register { server ->
        ModDataTracker.tick(server)
    }
}
