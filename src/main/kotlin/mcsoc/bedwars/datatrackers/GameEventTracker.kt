package mcsoc.bedwars.datatrackers

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import mcsoc.bedwars.gamestate.GameEvent
import net.minecraft.server.level.ServerLevel
import java.util.PriorityQueue
import java.util.UUID


class GameEventTracker() : LevelTiedData() {
    companion object {
        val CODEC: MapCodec<GameEventTracker> = RecordCodecBuilder.mapCodec{it.group(
            Codec.LONG.fieldOf("tickCount").forGetter(GameEventTracker::currTickNumber),
            GameEvent.CODEC.listOf().fieldOf("queuedEvents").forGetter(GameEventTracker::listQueuedEvents)
        ).apply(it, ::GameEventTracker)}
    }
    override val type get() = LevelDataType.EventQueue
    
    lateinit var level: ServerLevel
    private var currTickNumber: Long = 0
    val eventQueue = PriorityQueue<GameEvent>()
    private fun listQueuedEvents(): List<GameEvent> = eventQueue.toList()
    
    private constructor(currTickNumber: Long, eventList: List<GameEvent>) : this() {
        this.currTickNumber = currTickNumber
        this.eventQueue.addAll(eventList)
    }

    private fun queueEvent(event: GameEvent) {
        eventQueue.add(event)
    }
    
    fun queueEntityExpiry(ticks: Long, uuid: UUID) {
        queueEvent(GameEvent.EntityExpiryEvent(currTickNumber + ticks, uuid))
    }

    private fun dequeueEventsToTrigger(): Collection<GameEvent> {
        val eventsToTrigger = mutableSetOf<GameEvent>()
        while (!eventQueue.isEmpty() && eventQueue.peek().hasExpired(currTickNumber))
            eventsToTrigger.add(eventQueue.poll())
        return eventsToTrigger
    }

    fun tick() {
        currTickNumber++
        for (event in dequeueEventsToTrigger())
            event.trigger(level)
    }
}