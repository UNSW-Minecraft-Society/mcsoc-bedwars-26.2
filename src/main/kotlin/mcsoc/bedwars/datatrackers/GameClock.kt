package mcsoc.bedwars.datatrackers

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import mcsoc.bedwars.utils.CODEC
import mcsoc.bedwars.utils.inWholeTicks
import kotlin.time.Duration
import kotlin.time.TimeSource


interface TimeHolder : Comparable<TimeHolder> {
    val time: Duration
    override fun compareTo(other: TimeHolder): Int = time.compareTo(other.time)
}

interface TickExposer : TimeHolder {
    fun tick()
    fun reset()
    
    // TimerTick exposes the ticks since last tick() invocation
    val timerTick: Int
    // TimerSecond exposes the seconds since last tick() invocation
    val timerSecond: Int
}

private interface TickHolder : TickExposer


private class HiddenGameTicker() : TickHolder {
    companion object {
        val CODEC: Codec<HiddenGameTicker> = RecordCodecBuilder.create{it.group(
            Duration.CODEC.fieldOf("time").forGetter(HiddenGameTicker::time)
        ).apply(it, ::HiddenGameTicker)}
    }
    private constructor(time: Duration) : this() {
        this.time = time
    }
    
    override var time: Duration = Duration.ZERO
    var prevTickTime = TimeSource.Monotonic.markNow()
    override var timerTick: Int = 0
    override var timerSecond: Int = 0
    
    override fun tick() {
        if (prevTickTime.hasNotPassedNow()) return
        
        var tickDelta = prevTickTime.elapsedNow()
        val oldTime = time
        time += tickDelta
        timerTick = (time.inWholeTicks - oldTime.inWholeTicks).toInt()
        timerSecond = (time.inWholeSeconds - oldTime.inWholeSeconds).toInt()
        
        prevTickTime = TimeSource.Monotonic.markNow()
    }
    override fun reset() {
        this.time = Duration.ZERO
    }
}

class GameTimer() : LevelTiedData(), TickExposer {
    private var timer: HiddenGameTicker = HiddenGameTicker()
    companion object {
        val CODEC: MapCodec<GameTimer> = RecordCodecBuilder.mapCodec{it.group(
            HiddenGameTicker.CODEC.fieldOf("timer").forGetter(GameTimer::timer)
        ).apply(it, ::GameTimer)}
    }
    private constructor(timer: HiddenGameTicker) : this() {
        this.timer = timer
    }

    override val type: LevelDataType<*> get() = LevelDataType.GameClock
    
    override val time: Duration get() = timer.time
    override val timerTick: Int get() = timer.timerTick
    override val timerSecond: Int get() = timer.timerSecond
    
    override fun tick() = timer.tick()
    override fun reset() = timer.reset()
}