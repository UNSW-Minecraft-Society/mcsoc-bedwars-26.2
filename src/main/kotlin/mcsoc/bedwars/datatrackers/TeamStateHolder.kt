package mcsoc.bedwars.datatrackers

import mcsoc.bedwars.generators.BaseGenerator
import mcsoc.bedwars.generators.Generator
import mcsoc.bedwars.utils.Team
import net.minecraft.world.phys.Vec3
import java.util.UUID

internal interface TeamStateRecord {
    fun getPlayers(): List<UUID>
    fun getBedAlive(): Boolean
    fun getSpawn(): Vec3
    fun getGenerator(): BaseGenerator?

    fun setBedAlive(bedAlive: Boolean)
    fun addPlayer(player: UUID)
    fun upgradeGenerator()
    fun setGenerator(gen: BaseGenerator)
}

internal interface PlayerTeamState {
    fun setTeamName(team: Team)
    fun getTeamName(): Team
}

internal interface TeamStateExposer {
    fun getPlayersInTeam(team: Team): List<UUID>
    fun getBedDestroyed(team: Team): Boolean
    fun getTeamSpawn(team: Team): Vec3
    fun getActiveTeams(): List<Team>

    fun setBedAlive(team: Team, state: Boolean)
    fun addPlayer(player: UUID, team: Team)
    fun initialiseTeams(numTeams: Int)

    fun getPlayersTeam(player: UUID): Team
    
    fun getActivePlayers(): Set<UUID>
    fun addActivePlayer(uuid: UUID): Boolean
    fun removeActivePlayer(uuid: UUID): Boolean
}

internal interface TeamStateHolder : TeamStateExposer {
    fun getTeam(team: Team): TeamStateRecord

    override fun getBedDestroyed(team: Team): Boolean = getTeam(team).getBedAlive()
    override fun getTeamSpawn(team: Team): Vec3 = getTeam(team).getSpawn()
    override fun getPlayersInTeam(team: Team): List<UUID> = getTeam(team).getPlayers()
    override fun setBedAlive(team: Team, state: Boolean) = getTeam(team).setBedAlive(state)
}