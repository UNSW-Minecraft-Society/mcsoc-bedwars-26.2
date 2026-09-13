package mcsoc.bedwars.datatrackers

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import java.util.UUID

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
    fun getPlayerKills(uuid: UUID): Int
    fun getPlayerFinalKills(uuid: UUID): Int
    fun getPlayerDeaths(uuid: UUID): Int
    fun getPlayerBedsDestroyed(uuid: UUID): Int
    fun setPlayerKills(uuid: UUID, value: Int)
    fun setPlayerFinalKills(uuid: UUID, value: Int)
    fun setPlayerDeaths(uuid: UUID, value: Int)
    fun setPlayerBedsDestroyed(uuid: UUID, value: Int)
    
    fun incrementPlayerKills(uuid: UUID) = setPlayerKills(uuid, getPlayerKills(uuid) + 1)
    fun incrementPlayerFinalKills(uuid: UUID) = setPlayerFinalKills(uuid, getPlayerFinalKills(uuid) + 1)
    fun incrementPlayerDeaths(uuid: UUID) = setPlayerDeaths(uuid, getPlayerDeaths(uuid) + 1)
    fun incrementPlayerBedsDestroyed(uuid: UUID) = setPlayerBedsDestroyed(uuid, getPlayerBedsDestroyed(uuid) + 1)
}

internal interface PlayerStatsHolder : PlayerStatsExposer {
    fun getPlayerStats(uuid: UUID): PlayerStatsRecord

    override fun getPlayerKills(uuid: UUID): Int {
        return getPlayerStats(uuid).getKills()
    }

    override fun getPlayerFinalKills(uuid: UUID): Int {
        return getPlayerStats(uuid).getFinalKills()
    }

    override fun getPlayerDeaths(uuid: UUID): Int {
        return getPlayerStats(uuid).getDeaths()
    }

    override fun getPlayerBedsDestroyed(uuid: UUID): Int {
        return getPlayerStats(uuid).getBedsDestroyed()
    }

    override fun setPlayerKills(uuid: UUID, value: Int) {
        getPlayerStats(uuid).setKills(value)
    }

    override fun setPlayerFinalKills(uuid: UUID, value: Int) {
        getPlayerStats(uuid).setFinalKills(value)
    }

    override fun setPlayerDeaths(uuid: UUID, value: Int) {
        getPlayerStats(uuid).setDeaths(value)
    }

    override fun setPlayerBedsDestroyed(uuid: UUID, value: Int) {
        getPlayerStats(uuid).setBedsDestroyed(value)
    }
}
