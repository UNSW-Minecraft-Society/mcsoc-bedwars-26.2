package mcsoc.bedwars.eventhandlers

import mcsoc.bedwars.datatrackers.ModDataTracker
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import kotlin.time.Clock
import kotlin.time.Instant

object ServerTickEvent {
    lateinit var prev: Instant

    fun registerHandler() {
        ServerLifecycleEvents.SERVER_STARTED.register {
            prev = Clock.System.now()
        }

        ServerTickEvents.START_LEVEL_TICK.register { level ->
            val cur = Clock.System.now()
            ModDataTracker.tickGenerators(cur - prev)
            prev = cur
        }
    }
}