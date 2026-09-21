package mcsoc.bedwars.datatrackers

import java.util.UUID


interface PlayerInvisExposer {
    val isInvis: Boolean
}


interface PlayerInvisHolder : PlayerInvisExposer {
    override var isInvis: Boolean
}


interface PlayerInvisSwitchExposer {
    fun setPlayerInvisibility(player: UUID, invis: Boolean = true)
    fun getPlayerInvisibility(player: UUID): Boolean
}

internal interface PlayerInvisSwitcher : PlayerInvisSwitchExposer {
    fun getPlayerState(player: UUID): PlayerInvisHolder
    
    override fun setPlayerInvisibility(player: UUID, invis: Boolean) {
        getPlayerState(player).isInvis = invis
    }
    override fun getPlayerInvisibility(player: UUID): Boolean = getPlayerState(player).isInvis
}