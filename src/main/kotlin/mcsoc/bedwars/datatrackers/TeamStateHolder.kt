package mcsoc.bedwars.datatrackers

import mcsoc.bedwars.utils.Team
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.phys.Vec3
import kotlin.uuid.Uuid

internal interface TeamStateRecord {
    fun getPlayers(): MutableList<Uuid>
    fun getBedAlive(): Boolean
    fun getSpawn(): Vec3

    fun setBedAlive(bedAlive: Boolean)
    fun addPlayer(player: Uuid)
}

internal interface PlayerTeamState {
    fun setTeamName(team: Team)
    fun getTeamName(): Team
}

internal interface TeamStateExposer {
    fun getPlayersInTeam(team: Team): List<Uuid>
    fun getBedDestroyed(team: Team): Boolean
    fun getTeamSpawn(team: Team): Vec3
    fun getActiveTeams(): List<Team>

    fun setBedAlive(team: Team, state: Boolean)
    fun addPlayer(player: Uuid, team: Team)
    fun initialiseTeams(numTeams: Int)

    fun getPlayersTeam(player: Uuid): Team
}

internal interface TeamStateHolder : TeamStateExposer {
    fun getTeam(team: Team): TeamStateRecord

    override fun getBedDestroyed(team: Team): Boolean = getTeam(team).getBedAlive()
    override fun getTeamSpawn(team: Team): Vec3 = getTeam(team).getSpawn()
    override fun getPlayersInTeam(team: Team): List<Uuid> = getTeam(team).getPlayers()
    override fun setBedAlive(team: Team, state: Boolean) = getTeam(team).setBedAlive(state)
}

