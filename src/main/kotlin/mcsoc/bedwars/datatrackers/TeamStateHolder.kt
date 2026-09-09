package mcsoc.bedwars.datatrackers

import mcsoc.bedwars.utils.Team
import net.minecraft.world.phys.Vec3
import java.util.UUID

internal interface TeamStateRecord {
    fun getPlayers(): List<UUID>
    fun getBedAlive(): Boolean
    fun getBedBreaker(): UUID?
    fun getSpawn(): Vec3

    fun setBedAlive(bedAlive: Boolean)
    fun setBedBreaker(bedBreaker: UUID)
    fun addPlayer(player: UUID)
}

internal interface PlayerTeamState {
    fun setTeamName(team: Team)
    fun getTeamName(): Team
}

internal interface TeamStateExposer {
    fun getPlayersInTeam(team: Team): List<UUID>
    fun getBedDestroyed(team: Team): Boolean
    fun getBedBreaker(team: Team): UUID?
    fun getTeamSpawn(team: Team): Vec3
    fun getActiveTeams(): List<Team>

    fun setBedAlive(team: Team, state: Boolean)
    fun setBedBreaker(team: Team, player: UUID)
    fun addPlayer(player: UUID, team: Team)
    fun initialiseTeams(numTeams: Int)

    fun getPlayersTeam(player: UUID): Team
    
    fun getActivePlayers(): Set<UUID>
    fun addActivePlayer(uuid: UUID): Boolean
    fun removeActivePlayer(uuid: UUID): Boolean
    fun clearActivePlayers()
}

internal interface TeamStateHolder : TeamStateExposer {
    fun getTeam(team: Team): TeamStateRecord

    override fun getBedDestroyed(team: Team): Boolean = !getTeam(team).getBedAlive()
    override fun getBedBreaker(team: Team): UUID? = getTeam(team).getBedBreaker()
    override fun getTeamSpawn(team: Team): Vec3 = getTeam(team).getSpawn()
    override fun getPlayersInTeam(team: Team): List<UUID> = getTeam(team).getPlayers()
    override fun setBedAlive(team: Team, state: Boolean) = getTeam(team).setBedAlive(state)
    override fun setBedBreaker(team: Team, player: UUID) = getTeam(team).setBedBreaker(player)
}