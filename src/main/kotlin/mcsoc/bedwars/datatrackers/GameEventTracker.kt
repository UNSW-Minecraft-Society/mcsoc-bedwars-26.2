package mcsoc.bedwars.datatrackers

import mcsoc.bedwars.gamestate.GameEvent
import net.minecraft.server.level.ServerLevel
import java.util.PriorityQueue

sealed class GameEventTracker {
    val level: ServerLevel
    private var currTickNumber: Int = 0
    val eventQueue = PriorityQueue<GameEvent>()

    protected constructor(level: ServerLevel) {
        this.level = level
    }

    fun queueEvent(event: GameEvent) {
        eventQueue.add(event)
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