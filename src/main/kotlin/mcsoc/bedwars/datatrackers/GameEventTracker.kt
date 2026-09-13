package mcsoc.bedwars.datatrackers

import mcsoc.bedwars.gamestate.GameEvent
import net.minecraft.server.level.ServerLevel
import java.util.PriorityQueue

data class ScheduledGameEvent(val gameEvent: GameEvent, val tickNumber: Int) : Comparable<ScheduledGameEvent> {
    override fun compareTo(other: ScheduledGameEvent): Int {
        return tickNumber.compareTo(other.tickNumber)
    }
}

sealed class GameEventTracker {
    val level: ServerLevel
    private var tickNumber: Int = 0
    val eventQueue = PriorityQueue<ScheduledGameEvent>()

    protected constructor(level: ServerLevel) {
        this.level = level
    }

    fun queueEvent(event: GameEvent, numTicks: Int) {
        eventQueue.add(ScheduledGameEvent(event, tickNumber + numTicks))
    }

    private fun dequeueEventsToTrigger(): Collection<GameEvent> {
        val eventsToTrigger = mutableSetOf<GameEvent>()
        while (!eventQueue.isEmpty() && eventQueue.peek().tickNumber < tickNumber)
            eventsToTrigger.add(eventQueue.poll().gameEvent)
        return eventsToTrigger
    }

    fun tick() {
        tickNumber++
        for (event in dequeueEventsToTrigger())
            event.eventCallback(level)
    }
}