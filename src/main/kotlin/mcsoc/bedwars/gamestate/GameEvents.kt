package mcsoc.bedwars.gamestate

sealed interface GameEvent {
    val runnable: () -> Unit
}

