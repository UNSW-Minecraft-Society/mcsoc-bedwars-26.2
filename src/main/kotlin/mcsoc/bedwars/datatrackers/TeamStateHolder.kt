package mcsoc.bedwars.datatrackers

import mcsoc.bedwars.utils.Colour
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.phys.Vec3

internal interface TeamStateRecord {
    fun getColour(): Colour
    fun getPlayers(server: MinecraftServer): MutableList<ServerPlayer>
    fun getBedAlive(): Boolean
    fun getSpawn(): Vec3
    fun getMaxPlayers(): Int
    fun getPlayerCount(): Int

    fun setBedAlive(bedAlive: Boolean)
    fun addPlayer(player: ServerPlayer)
}

internal interface PlayerTeamState {
    fun getTeam(): Colour
}

internal interface TeamStateExposer {
    fun getPlayersInTeam(colour: Colour, level: ServerLevel): List<ServerPlayer>
    fun getBedDestroyed(colour: Colour): Boolean
    fun getTeamSpawn(colour: Colour): Vec3

    fun destroyBed(colour: Colour)
    fun addPlayer(player: ServerPlayer)
    fun addPlayerToTeam(player: ServerPlayer, team: Colour)
    fun createTeams(players: List<ServerPlayer>, numTeams: Int)
}

internal interface TeamStateHolder : TeamStateExposer {
    fun getTeam(colour: Colour): TeamStateRecord

    override fun getBedDestroyed(colour: Colour): Boolean = getTeam(colour).getBedAlive()
    override fun getTeamSpawn(colour: Colour): Vec3 = getTeam(colour).getSpawn()
    override fun getPlayersInTeam(colour: Colour, level: ServerLevel): List<ServerPlayer> =
        getTeam(colour).getPlayers(level.server)

    override fun destroyBed(colour: Colour) {
        getTeam(colour).setBedAlive(false)
        // other things related to bed destruction like title and sound
    }

    override fun addPlayerToTeam(player: ServerPlayer, team: Colour) {
        getTeam(team).addPlayer(player)
    }
}

