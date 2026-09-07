package mcsoc.bedwars.eventhandlers

import mcsoc.bedwars.datatrackers.ModDataTracker
import mcsoc.bedwars.datatrackers.generatorState
import mcsoc.bedwars.gamestate.GameManager
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents

fun registerEventHandlers() {
    ServerTickEvents.END_LEVEL_TICK.register{ level ->
        GameManager.tick(level)
    }

    ServerLifecycleEvents.SERVER_STARTED.register { server ->
        server.allLevels.forEach {
            it.generatorState.placeGenerators(server)
        }
    }

    ServerLifecycleEvents.SERVER_STOPPING.register {server ->
        server.allLevels.forEach {
            it.generatorState.removeTimerEntities()
        }
    }

    registerItemCallbacks()
    registerEntityCallbacks()
    registerPlayerJoinEvent()
    registerBlockBreakEvents()
}
