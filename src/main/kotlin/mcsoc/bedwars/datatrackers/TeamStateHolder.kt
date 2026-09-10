package mcsoc.bedwars.datatrackers

import mcsoc.bedwars.utils.Team
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import java.util.UUID

internal interface TeamStateRecord {
    fun getPlayers(): List<UUID>
    fun getBedAlive(): Boolean
    fun getBedPosition(): BlockPos
    fun getSpawn(): Vec3

    fun setSpawn(pos: Vec3)
    fun setBedAlive(bedAlive: Boolean)
    fun setBedPosition(pos: BlockPos)
    fun addPlayer(player: UUID)
}

internal interface PlayerTeamState {
    fun setTeamName(team: Team)
    fun getTeamName(): Team
}

internal interface TeamStateExposer {
    fun getPlayersInTeam(team: Team): List<UUID>
    fun getBedDestroyed(team: Team): Boolean
    fun getTeamSpawn(team: Team): Vec3
    fun getTeamBedPosition(team: Team): BlockPos
    fun getActiveTeams(): List<Team>

    fun setBedAlive(team: Team, state: Boolean)
    fun setTeamSpawn(team: Team, pos: Vec3) 
    fun setTeamBedPosition(team: Team, pos: BlockPos)
    fun addPlayer(player: UUID, team: Team)
    fun initialiseTeams(numTeams: Int)
    fun initialiseTeams(teams: Set<Team>)

    fun getPlayersTeam(player: UUID): Team
    
    fun getActivePlayers(): Set<UUID>
    fun addActivePlayer(uuid: UUID): Boolean
    fun removeActivePlayer(uuid: UUID): Boolean
    fun clearActivePlayers()
}

internal interface TeamStateHolder : TeamStateExposer {
    fun getTeam(team: Team): TeamStateRecord

    override fun getBedDestroyed(team: Team): Boolean = !getTeam(team).getBedAlive()
    override fun getTeamSpawn(team: Team): Vec3 = getTeam(team).getSpawn()
    override fun setTeamSpawn(team: Team, pos: Vec3) = getTeam(team).setSpawn(pos)
    override fun getTeamBedPosition(team: Team): BlockPos = getTeam(team).getBedPosition()
    override fun setTeamBedPosition(team: Team, pos: BlockPos) = getTeam(team).setBedPosition(pos)
    override fun getPlayersInTeam(team: Team): List<UUID> = getTeam(team).getPlayers()
    override fun setBedAlive(team: Team, state: Boolean) = getTeam(team).setBedAlive(state)
}