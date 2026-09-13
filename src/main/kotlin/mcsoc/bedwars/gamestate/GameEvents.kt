package mcsoc.bedwars.gamestate

import net.minecraft.server.level.ServerLevel
import java.util.UUID

sealed class GameEvent(private val tickNumber: Int): Comparable<GameEvent> {
    // add codec here

    abstract fun trigger(level: ServerLevel)

    fun hasExpired(currTickNumber: Int): Boolean = tickNumber <= currTickNumber

    override fun compareTo(other: GameEvent): Int {
        return tickNumber.compareTo(other.tickNumber)
    }

    data class GolemExpiryEvent(val tickNumber: Int, val entityId: UUID): GameEvent(tickNumber) {
        override fun trigger(level: ServerLevel) {
            TODO("Not yet implemented")
        }
    }
}

