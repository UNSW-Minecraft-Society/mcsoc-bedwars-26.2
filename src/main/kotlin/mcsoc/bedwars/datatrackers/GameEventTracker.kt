package mcsoc.bedwars.datatrackers

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import mcsoc.bedwars.BedwarsPlugin
import mcsoc.bedwars.gamestate.GameEvent
import mcsoc.bedwars.utils.INSTANT_CODEC
import mcsoc.bedwars.utils.ticks
import net.minecraft.server.level.ServerLevel
import java.util.PriorityQueue
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.TimeSource


class GameEventTracker() : LevelTiedData() {
    companion object {
        val CODEC: MapCodec<GameEventTracker> = RecordCodecBuilder.mapCodec{it.group(
            INSTANT_CODEC.fieldOf("counter").forGetter(GameEventTracker::currTime),
            GameEvent.CODEC.listOf().fieldOf("queuedEvents").forGetter(GameEventTracker::listQueuedEvents)
        ).apply(it, ::GameEventTracker)}
    }
    override val type get() = LevelDataType.EventQueue
    
    lateinit var level: ServerLevel
    private var currTime: Instant = Instant.fromEpochMilliseconds(0)
    private val eventQueue = PriorityQueue<GameEvent>()
    private fun listQueuedEvents(): List<GameEvent> = eventQueue.toList()
    
    private constructor(currTime: Instant, eventList: List<GameEvent>) : this() {
        this.currTime = currTime
        for (event in eventList) {
            queueEvent(event)
        }
    }

    private fun queueEvent(event: GameEvent) {
        eventQueue.add(event)
    }
    
    fun queueEntityExpiry(ticks: Long, uuid: UUID) {
        queueEvent(GameEvent.EntityExpiryEvent(currTime + ticks.ticks, uuid))
    }

    private fun dequeueEventsToTrigger(): Iterable<GameEvent> {
        val eventsToTrigger: MutableCollection<GameEvent> = mutableSetOf()
        while (eventQueue.isNotEmpty() && eventQueue.peek().hasExpired(currTime))
            eventsToTrigger.add(eventQueue.poll() ?: break)
        return eventsToTrigger
    }

    fun reset() {
        this.eventQueue.clear()
    }
    
    fun tick() {
        currTime += 1.ticks
        for (event in dequeueEventsToTrigger())
            event.trigger(level)
    }
}