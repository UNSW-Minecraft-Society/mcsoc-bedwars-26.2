package mcsoc.bedwars.datatrackers

import mcsoc.bedwars.BedwarsPlugin
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player

internal interface PlayerTimeRecord : TickExposer

internal interface PlayerTimeExposer {
    fun tick()
    fun getPlayerRespawnSeconds(player: ServerPlayer): Int
    fun resetPlayerRespawnTime(player: ServerPlayer)
    fun playerTimerSecondPassed(player: ServerPlayer): Int
}

internal interface PlayerTimeHolder : PlayerTimeExposer {
    fun getPlayerTime(player: Player): PlayerTimeRecord

    override fun getPlayerRespawnSeconds(player: ServerPlayer): Int {
        return getPlayerTime(player).time.inWholeSeconds.toInt() + 1
    }

    override fun resetPlayerRespawnTime(player: ServerPlayer) {
        getPlayerTime(player).reset()
    }

    override fun playerTimerSecondPassed(player: ServerPlayer) = getPlayerTime(player).timerSecond
}
