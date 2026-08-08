package mcsoc.bedwars.datatrackers

import mcsoc.bedwars.utils.Team
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.phys.Vec3

internal interface TeamStateRecord {
    fun getPlayers(server: MinecraftServer): MutableList<ServerPlayer>
    fun getBedAlive(): Boolean
    fun getSpawn(): Vec3
    fun getMaxPlayers(): Int
    fun getPlayerCount(): Int

    fun setBedAlive(bedAlive: Boolean)
    fun addPlayer(player: ServerPlayer)
}

internal interface PlayerTeamState {
    fun setTeam(team: Team)
    fun getTeam(): Team
}

internal interface TeamStateExposer {
    fun getPlayersInTeam(team: Team, level: ServerLevel): List<ServerPlayer>
    fun getBedDestroyed(team: Team): Boolean
    fun getTeamSpawn(team: Team): Vec3

    fun destroyBed(team: Team)
    fun addPlayer(player: ServerPlayer)
    fun addPlayerToTeam(player: ServerPlayer, team: Team)
    fun createTeams(players: List<ServerPlayer>, numTeams: Int)
    
    fun getPlayersTeam(player: ServerPlayer): Team
}

internal interface TeamStateHolder : TeamStateExposer {
    fun getTeam(team: Team): TeamStateRecord

    override fun getBedDestroyed(team: Team): Boolean = getTeam(team).getBedAlive()
    override fun getTeamSpawn(team: Team): Vec3 = getTeam(team).getSpawn()
    override fun getPlayersInTeam(team: Team, level: ServerLevel): List<ServerPlayer> =
        getTeam(team).getPlayers(level.server)

    override fun destroyBed(team: Team) {
        getTeam(team).setBedAlive(false)
        // other things related to bed destruction like title and sound
    }

    override fun addPlayerToTeam(player: ServerPlayer, team: Team) {
        getTeam(team).addPlayer(player)
    }
}

