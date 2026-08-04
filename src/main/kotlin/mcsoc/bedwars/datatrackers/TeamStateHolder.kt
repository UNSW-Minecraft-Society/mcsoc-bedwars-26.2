package mcsoc.bedwars.datatrackers

import mcsoc.bedwars.utils.Colour
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.phys.Vec3

internal data class TeamDataRecord(
    // TODO create codec for this
    val colour: Colour,
    val players: MutableList<ServerPlayer>,
    var bedAlive: Boolean,
    var spawn: Vec3,
    val maxPlayers: Int
)

internal interface TeamStateExposer {
    fun getPlayersInTeam(colour: Colour): List<ServerPlayer>
    fun getBedDestroyed(colour: Colour): Boolean
    fun getTeamSpawn(colour: Colour): Vec3
    
    fun destroyBed(colour: Colour)
    fun addPlayerToTeam(player: ServerPlayer, team: Colour)
    fun createTeams(players: List<ServerPlayer>, numTeams: Int)
}

internal interface TeamStateHolder : TeamStateExposer {
    fun getTeam(colour: Colour): TeamDataRecord

    override fun getBedDestroyed(colour: Colour): Boolean = getTeam(colour).bedAlive
    override fun getPlayersInTeam(colour: Colour): List<ServerPlayer> = getTeam(colour).players
    override fun getTeamSpawn(colour: Colour): Vec3 = getTeam(colour).spawn

    override fun destroyBed(colour: Colour) {
        getTeam(colour).bedAlive = false
        
        // other things related to bed destruction like title and sound
    }

    override fun addPlayerToTeam(player: ServerPlayer, team: Colour) {
        getTeam(team).players.add(player)
    }
}

