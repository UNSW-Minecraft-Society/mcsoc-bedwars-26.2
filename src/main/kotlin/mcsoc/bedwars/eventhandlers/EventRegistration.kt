package mcsoc.bedwars.eventhandlers

import mcsoc.bedwars.datatrackers.ModDataTracker
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents

class EventRegistration {
    companion object {
        fun registerEventHandlers() {
            ServerTickEvents.END_SERVER_TICK.register{server ->
                ModDataTracker.tick()
            }
        }
    }
}