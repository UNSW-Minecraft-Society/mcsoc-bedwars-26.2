package mcsoc.bedwars.datatrackers

import net.minecraft.server.MinecraftServer
import kotlin.time.Duration

internal interface TickExposer {
    fun tick()
    fun getGameTime(): Duration
    fun resetGameTime()

    // TimerTick registers as true once every tick
    fun getTimerTick(): Boolean

    // TimerSecond registers as true once every second
    fun getTimerSecond(): Boolean
}

internal interface Ticker : TickExposer