package mcsoc.bedwars.datatrackers

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player

internal interface PlayerTimeRecord {
    fun getRespawnSeconds(): Int
    fun getSecondPassed(): Boolean
    fun decrementPlayerRespawnTicks()
    fun resetPlayerRespawnTime()
}

internal interface PlayerTimeExposer {
    fun getPlayerRespawnSeconds(player: ServerPlayer): Int
    fun resetPlayerRespawnTime(player: ServerPlayer)
    fun playerTimerSecondPassed(player: ServerPlayer): Boolean
}

internal interface PlayerTimeHolder : PlayerTimeExposer {
    fun getPlayerTime(player: Player): PlayerTimeRecord

    override fun getPlayerRespawnSeconds(player: ServerPlayer): Int {
        return getPlayerTime(player).getRespawnSeconds()
    }

    override fun resetPlayerRespawnTime(player: ServerPlayer) {
        getPlayerTime(player).resetPlayerRespawnTime()
    }

    override fun playerTimerSecondPassed(player: ServerPlayer): Boolean {
        return getPlayerTime(player).getSecondPassed()
    }
}
