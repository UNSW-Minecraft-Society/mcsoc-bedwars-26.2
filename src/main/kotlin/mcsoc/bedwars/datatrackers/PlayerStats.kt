package mcsoc.bedwars.datatrackers

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player

internal interface PlayerStatsRecord {
    fun getKills(): Int
    fun getFinalKills(): Int
    fun getDeaths(): Int
    fun getBedsDestroyed(): Int
    fun setKills(value: Int)
    fun setFinalKills(value: Int)
    fun setDeaths(value: Int)
    fun setBedsDestroyed(value: Int)
}

internal interface PlayerStatsExposer {
    fun getPlayerKills(player: ServerPlayer): Int
    fun getPlayerFinalKills(player: ServerPlayer): Int
    fun getPlayerDeaths(player: ServerPlayer): Int
    fun getPlayerBedsDestroyed(player: ServerPlayer): Int
    fun setPlayerKills(player: ServerPlayer, value: Int)
    fun setPlayerFinalKills(player: ServerPlayer, value: Int)
    fun setPlayerDeaths(player: ServerPlayer, value: Int)
    fun setPlayerBedsDestroyed(player: ServerPlayer, value: Int)
}

internal interface PlayerStatsHolder : PlayerStatsExposer {
    fun getPlayerStats(player: Player): PlayerStatsRecord

    override fun getPlayerKills(player: ServerPlayer): Int {
        return getPlayerStats(player).getKills()
    }

    override fun getPlayerFinalKills(player: ServerPlayer): Int {
        return getPlayerStats(player).getFinalKills()
    }

    override fun getPlayerDeaths(player: ServerPlayer): Int {
        return getPlayerStats(player).getDeaths()
    }

    override fun getPlayerBedsDestroyed(player: ServerPlayer): Int {
        return getPlayerStats(player).getBedsDestroyed()
    }

    override fun setPlayerKills(player: ServerPlayer, value: Int) {
        getPlayerStats(player).setKills(value)
    }

    override fun setPlayerFinalKills(player: ServerPlayer, value: Int) {
        getPlayerStats(player).setFinalKills(value)
    }

    override fun setPlayerDeaths(player: ServerPlayer, value: Int) {
        getPlayerStats(player).setDeaths(value)
    }

    override fun setPlayerBedsDestroyed(player: ServerPlayer, value: Int) {
        getPlayerStats(player).setBedsDestroyed(value)
    }
}
