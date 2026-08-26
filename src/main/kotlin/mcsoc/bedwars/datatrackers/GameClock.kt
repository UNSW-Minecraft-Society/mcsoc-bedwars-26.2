package mcsoc.bedwars.datatrackers

import kotlin.time.Duration

internal interface TickExposer {
    fun tick()
    fun getGameTime(): Duration
    
    // TimerTick registers as true once every tick
    fun getTimerTick(): Boolean
}

internal interface Ticker : TickExposer