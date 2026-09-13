package mcsoc.bedwars.gamestate

import net.minecraft.server.level.ServerLevel

sealed interface GameEvent {
    val eventCallback: (ServerLevel) -> Unit
}

