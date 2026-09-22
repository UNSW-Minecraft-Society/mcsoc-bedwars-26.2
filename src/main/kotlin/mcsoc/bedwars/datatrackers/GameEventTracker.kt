package mcsoc.bedwars.datatrackers

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import mcsoc.bedwars.gamestate.GameEvent
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.state.BlockState
import java.util.PriorityQueue
import java.util.UUID
import kotlin.time.Duration


class GameEventTracker() : LevelTiedData() {
    companion object {
        val CODEC: MapCodec<GameEventTracker> = RecordCodecBuilder.mapCodec{it.group(
            GameEvent.CODEC.listOf().fieldOf("queuedEvents").forGetter(GameEventTracker::listQueuedEvents)
        ).apply(it, ::GameEventTracker)}
    }
    override fun getType() = LevelDataType.EventQueue
    
    lateinit var level: ServerLevel
    private val eventQueue = PriorityQueue<GameEvent>()
    private fun listQueuedEvents(): List<GameEvent> = eventQueue.toList()
    
    private constructor(eventList: List<GameEvent>) : this() {
        queueEvents(eventList)
    }
    
    fun queueEvent(event: GameEvent) {
        eventQueue.add(event)
    }
    fun queueEvents(events: Iterable<GameEvent>) {
        eventQueue.addAll(events)
    }
    
    fun queueEntityExpiry(lifetime: Duration, uuid: UUID) {
        queueEvent(GameEvent.EntityExpiryEvent(level.clock.time, lifetime, uuid))
    }
    fun queueGameStartCounter(startTime: Duration) {
        queueEvent(GameEvent.GameStartCounterEvent(level.clock.time, startTime))
    }
    fun queuePlayerRespawn(respawnTime: Duration, uuid: UUID) {
        queueEvent(GameEvent.RespawnCounterEvent(level.clock.time, respawnTime, uuid))
    }
    fun queuePopupTowerConstruction(centrePos: BlockPos, buildingBlockState: BlockState, orientation: Direction) {
        queueEvent(GameEvent.PopupTowerConstructionEvent(level.clock.time, centrePos, buildingBlockState, orientation))
    }

    private fun dequeueEventsToTrigger(): Iterable<GameEvent> {
        val eventsToTrigger: MutableCollection<GameEvent> = mutableSetOf()
        while (eventQueue.isNotEmpty() && eventQueue.peek().hasExpired(level.clock.time))
            eventsToTrigger.add(eventQueue.poll() ?: break)
        return eventsToTrigger
    }

    fun reset() {
        this.eventQueue.clear()
    }
    
    fun tick() {
        for (event in dequeueEventsToTrigger())
            event.trigger(level)
    }
}