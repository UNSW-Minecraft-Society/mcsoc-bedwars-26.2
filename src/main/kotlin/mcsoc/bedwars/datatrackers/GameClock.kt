package mcsoc.bedwars.datatrackers

import kotlin.time.Duration

interface TickExposer {
    fun tick()
    fun getGameTime(): Duration
    
    // TimerTick registers as true once every tick
    fun getTimerTick(): Boolean
}

interface Ticker : TickExposer