package mcsoc.bedwars.datatrackers

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import kotlinx.serialization.Serializable
import mcsoc.bedwars.utils.Team
import net.minecraft.core.UUIDUtil
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.saveddata.SavedData
import net.minecraft.world.phys.Vec3
import java.util.UUID
import kotlin.uuid.Uuid
import kotlin.uuid.toKotlinUuid


@Serializable
private class PlayerDataRecord() : PlayerStateRecord, PlayerTeamState {
    companion object {
        val CODEC: Codec<PlayerDataRecord> = RecordCodecBuilder.create{it.group(
            LifeState.CODEC.fieldOf("life_state").forGetter(PlayerDataRecord::life_state),
            Team.CODEC.fieldOf("team").forGetter(PlayerDataRecord::team)
        ).apply(it, ::PlayerDataRecord)}
    }

    private var life_state: LifeState = LifeState.ALIVE
    private var team: Team = Team.NONE

    private constructor(
        life_state: LifeState,
        team: Team
    ) : this() {
        this.life_state = life_state
        this.team = Team.NONE
    }

    override fun getLifeState(): LifeState {
        return this.life_state
    }

    override fun getTeam(): Team = team

    override fun setTeam(team: Team) {
        this.team = team
    }
}


private class TeamDataRecord(
    private val players: MutableList<UUID>,
    private var bedAlive: Boolean,
    private val spawn: Vec3,
    private val maxPlayers: Int
) : TeamStateRecord {
    companion object {
        val UUID_LIST_CODEC: Codec<MutableList<UUID>> = UUIDUtil.CODEC.listOf().xmap( { it.toMutableList() }, { it } )
        
        val CODEC: Codec<TeamDataRecord> = RecordCodecBuilder.create { it.group(
            UUID_LIST_CODEC.fieldOf("players").forGetter(TeamDataRecord::players),
            Codec.BOOL.fieldOf("bed_alive").forGetter(TeamDataRecord::bedAlive),
            Vec3.CODEC.fieldOf("spawn").forGetter(TeamDataRecord::spawn),
            Codec.INT.fieldOf("max_players").forGetter(TeamDataRecord::maxPlayers)
        ).apply(it, ::TeamDataRecord)}
    }

    override fun getBedAlive(): Boolean = bedAlive
    override fun getMaxPlayers(): Int = maxPlayers
    override fun getSpawn(): Vec3 = spawn
    override fun getPlayerCount() = players.size

    override fun getPlayers(server: MinecraftServer): MutableList<ServerPlayer> {
        return players
            .mapNotNull(server.playerList::getPlayer)
            .toMutableList()
    }

    override fun setBedAlive(bedAlive: Boolean) {
       this.bedAlive = bedAlive
    }

    override fun addPlayer(player: ServerPlayer) {
        players.add(player.uuid)
    }

}


private class ModDataStore() : SavedData(), PlayerStateHolder, TeamStateHolder {
    companion object {
        val UUIDCodec: Codec<Uuid> = Codec.STRING.xmap(Uuid::parse, Uuid::toString)
        
        val CODEC: Codec<ModDataStore> = RecordCodecBuilder.create{it.group(
            Codec.unboundedMap(UUIDCodec, PlayerDataRecord.CODEC)
                .fieldOf("player_data_map")
                .forGetter(ModDataStore::player_data_map),
                    
            Codec.unboundedMap(Team.CODEC, TeamDataRecord.CODEC)
                .fieldOf("teams_map")
                .forGetter(ModDataStore::teams_map)
        ).apply(it, ::ModDataStore)}
    }    
    
    private val player_data_map = HashMap<Uuid, PlayerDataRecord>()
    private val teams_map = HashMap<Team, TeamDataRecord>() // TODO add team_map to codec

    private constructor(
        playerMap: Map<Uuid, PlayerDataRecord>,
        teamMap: Map<Team, TeamDataRecord>
    ) : this() {
        this.player_data_map.putAll(playerMap)
        this.teams_map.putAll(teamMap)
    }

    private fun getPlayerData(id: Uuid): PlayerDataRecord {
        return player_data_map.getOrPut(id) { PlayerDataRecord() }
    }

    private fun getPlayerData(player: Player): PlayerDataRecord {
        return getPlayerData(player.uuid.toKotlinUuid())
    }

    override fun getPlayerState(player: Player): PlayerDataRecord {
        return getPlayerData(player)
    }

    override fun getTeam(team: Team): TeamDataRecord {
        return teams_map[team] ?: throw Exception("Invalid team")
    }

    override fun createTeams(players: List<ServerPlayer>, numTeams: Int) {
        assert(numTeams < Team.entries.size) { "More teams specified than can be handled" }
        
        val teams = Team.entries.take(numTeams)

        teams.forEach {
            // todo replace location and max people per team with config of some sorts
            teams_map[it] = TeamDataRecord(mutableListOf(), true, Vec3(0.0, 0.0, 0.0), 4)
        }

        players.forEachIndexed { i, player ->
            addPlayerToTeam(player, teams[i % numTeams])
            getPlayerData(player).setTeam(teams[i % numTeams])
        }
    }

    override fun addPlayer(player: ServerPlayer) {
        val team = teams_map.values
            .filter { it.getPlayerCount() < it.getMaxPlayers() }
            .minByOrNull { it.getPlayerCount() } ?: return

        team.addPlayer(player)
    }
    
    override fun getPlayersTeam(player: ServerPlayer): Team = getPlayerData(player).getTeam()
}


object ModDataTracker : PlayerStateExposer, TeamStateExposer {
    private val mod_data = ModDataStore()

    override fun isPlayerAlive(player: Player) = mod_data.isPlayerAlive(player)
    override fun isPlayerRespawning(player: Player) = mod_data.isPlayerRespawning(player)
    override fun isPlayerDead(player: Player) = mod_data.isPlayerDead(player)

    override fun getBedDestroyed(team: Team): Boolean = mod_data.getBedDestroyed(team)
    override fun getPlayersInTeam(team: Team, level: ServerLevel): List<ServerPlayer> = mod_data.getPlayersInTeam(team, level)
    override fun getTeamSpawn(team: Team): Vec3 = mod_data.getTeamSpawn(team)
    override fun addPlayerToTeam(player: ServerPlayer, team: Team) = mod_data.addPlayerToTeam(player, team)
    override fun destroyBed(team: Team) = mod_data.destroyBed(team)
    override fun createTeams(players: List<ServerPlayer>, numTeams: Int) = mod_data.createTeams(players, numTeams)
    override fun addPlayer(player: ServerPlayer) = mod_data.addPlayer(player)
    
    override fun getPlayersTeam(player: ServerPlayer): Team = mod_data.getPlayersTeam(player)
}