package mcsoc.bedwars.eventhandlers

import mcsoc.bedwars.datatrackers.ModDataTracker
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.world.level.Level
import kotlin.time.Clock
import kotlin.time.Instant

object ServerTickEvent {
    lateinit var prev: Instant

    fun registerHandler() {
        ServerLifecycleEvents.SERVER_STARTED.register {
            prev = Clock.System.now()
        }

        ServerTickEvents.START_LEVEL_TICK.register { level ->
            if (level.dimension() != Level.OVERWORLD) return@register

            val cur = Clock.System.now()
            // val diff = cur - prev
            ModDataTracker.tickGenerators(level)
            prev = cur
        }
    }
}