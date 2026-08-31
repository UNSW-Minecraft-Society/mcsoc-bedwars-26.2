package mcsoc.bedwars.datatrackers

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import kotlinx.serialization.Serializable
import mcsoc.bedwars.utils.inWholeTicks
import mcsoc.bedwars.utils.ticks
import kotlin.time.Duration
import kotlin.time.TimeSource
import mcsoc.bedwars.utils.Team
import net.minecraft.core.UUIDUtil
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.saveddata.SavedData
import net.minecraft.world.phys.Vec3
import java.util.UUID
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

enum class GamePhase {
    STARTING,
    ACTIVE,
    ENDED,
    INACTIVE
}

enum class GamePeriod {
    ACTIVE,
    DEATHMATCH,
    INACTIVE
}

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

    override fun getTeamName(): Team = team

    override fun setTeamName(team: Team) {
        this.team = team
    }
}


private class TeamDataRecord(
    private val players: MutableList<Uuid> = mutableListOf(),
    private var bedAlive: Boolean = true,
    private val spawn: Vec3 = Vec3(0.0, 0.0, 0.0),
) : TeamStateRecord {
    companion object {
        val UUID_LIST_CODEC: Codec<MutableList<Uuid>> = UUIDUtil.CODEC.listOf().xmap(
            { it.map(UUID::toKotlinUuid).toMutableList() },
            { it.map(Uuid::toJavaUuid) }
        )
        
        val CODEC: Codec<TeamDataRecord> = RecordCodecBuilder.create { it.group(
            UUID_LIST_CODEC.fieldOf("players").forGetter(TeamDataRecord::players),
            Codec.BOOL.fieldOf("bed_alive").forGetter(TeamDataRecord::bedAlive),
            Vec3.CODEC.fieldOf("spawn").forGetter(TeamDataRecord::spawn),
        ).apply(it, ::TeamDataRecord)}
    }

    override fun getBedAlive(): Boolean = bedAlive
    override fun getSpawn(): Vec3 = spawn
    override fun getPlayers(): MutableList<Uuid> = players

    override fun setBedAlive(bedAlive: Boolean) {
       this.bedAlive = bedAlive
    }

    override fun addPlayer(player: Uuid) {
        players.add(player)
    }
}


private class ModDataStore() : SavedData(), PlayerStateHolder, TeamStateHolder, Ticker {
    companion object {
        val UUIDCodec: Codec<Uuid> = Codec.STRING.xmap(Uuid::parse, Uuid::toString)
        
        val CODEC: Codec<ModDataStore> = RecordCodecBuilder.create{it.group(
            Codec.unboundedMap(UUIDCodec, PlayerDataRecord.CODEC)
                .fieldOf("player_data_map")
                .forGetter(ModDataStore::player_data_map),
                    
            Codec.unboundedMap(Team.CODEC, TeamDataRecord.CODEC)
                .fieldOf("teams_map")
                .forGetter(ModDataStore::teams_map),

            Codec.STRING.xmap(Duration::parseIsoString, Duration::toIsoString)
                .fieldOf("game_timer")
                .forGetter(ModDataStore::game_timer)
        ).apply(it, ::ModDataStore)}
    }    
    
    private val player_data_map = HashMap<Uuid, PlayerDataRecord>()
    private val teams_map = HashMap<Team, TeamDataRecord>()
    private val active_players = mutableSetOf<UUID>()
    private var prev_tick_time = TimeSource.Monotonic.markNow()
    private var tick_delta = Duration.ZERO
    private var game_timer = Duration.ZERO
    private var timer_tick = false
    private var timer_second = false
    private var game_phase = GamePhase.INACTIVE
    private var game_period = GamePeriod.INACTIVE
    
    private constructor(
        playerMap: Map<Uuid, PlayerDataRecord>,
        teamMap: Map<Team, TeamDataRecord>,
        timer: Duration
    ) : this() {
        this.player_data_map.putAll(playerMap)
        this.teams_map.putAll(teamMap)
        this.game_timer = timer
    }
    
    
    override fun tick() {
        tick_delta = prev_tick_time.elapsedNow()
        prev_tick_time = TimeSource.Monotonic.markNow()
        
        timer_tick = game_timer.inWholeTicks != (game_timer + tick_delta).inWholeTicks
        timer_second = game_timer.inWholeSeconds != (game_timer + tick_delta).inWholeSeconds
        game_timer += tick_delta
    }

    override fun getGameTime() = game_timer

    override fun resetGameTime() {game_timer = Duration.ZERO}
        
    override fun getTimerTick() = timer_tick

    override fun getTimerSecond() = timer_second

    fun getGamePhase(): GamePhase {
        return game_phase
    }

    fun setGamePhase(phase: GamePhase) {
        game_phase = phase
    }

    fun getGamePeriod(): GamePeriod {
        return game_period
    }

    fun setGamePeriod(period: GamePeriod) {
        game_period = period
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

    override fun getActiveTeams(): List<Team> = teams_map.keys.toList()

    // to be updated by map loader
    // this will reset team data
    override fun initialiseTeams(numTeams: Int) {
        assert(numTeams < Team.entries.size) { "More teams specified than can be handled" }
        teams_map.clear()
        
        val teams = Team.entries.take(numTeams)
        teams.forEach { teams_map[it] = TeamDataRecord() }
    }

    override fun addPlayer(player: Uuid, team: Team) {
        getTeam(team).addPlayer(player)
        getPlayerData(player).setTeamName(team)
    }
    
    override fun getPlayersTeam(player: Uuid): Team = getPlayerData(player).getTeamName()

    override fun addActivePlayer(uuid: UUID) = active_players.add(uuid)
    override fun removeActivePlayer(uuid: UUID) = active_players.remove(uuid)
    override fun getActivePlayers() = active_players
    override fun clearActivePlayers() = active_players.clear()
}


object ModDataTracker : PlayerStateExposer, TeamStateExposer, TickExposer {
    private val mod_data = ModDataStore()

    override fun tick() = mod_data.tick()
    override fun getGameTime(): Duration = mod_data.getGameTime()
    override fun resetGameTime() = mod_data.resetGameTime()
    override fun getTimerTick(): Boolean = mod_data.getTimerTick()
    override fun getTimerSecond(): Boolean = mod_data.getTimerSecond()

    fun getGamePhase(): GamePhase = mod_data.getGamePhase()
    fun setGamePhase(phase: GamePhase) = mod_data.setGamePhase(phase)
    fun getGamePeriod(): GamePeriod = mod_data.getGamePeriod()
    fun setGamePeriod(period: GamePeriod) = mod_data.setGamePeriod(period)

    override fun isPlayerAlive(player: Player) = mod_data.isPlayerAlive(player)
    override fun isPlayerRespawning(player: Player) = mod_data.isPlayerRespawning(player)
    override fun isPlayerDead(player: Player) = mod_data.isPlayerDead(player)

    override fun getBedDestroyed(team: Team): Boolean = mod_data.getBedDestroyed(team)
    override fun getPlayersInTeam(team: Team): List<Uuid> = mod_data.getPlayersInTeam(team)
    override fun getTeamSpawn(team: Team): Vec3 = mod_data.getTeamSpawn(team)
    override fun getActiveTeams(): List<Team> = mod_data.getActiveTeams()
    override fun setBedAlive(team: Team, state: Boolean) = mod_data.setBedAlive(team, state)
    override fun initialiseTeams(numTeams: Int) = mod_data.initialiseTeams(numTeams)
    override fun addPlayer(player: Uuid, team: Team) = mod_data.addPlayer(player, team)
    
    override fun getPlayersTeam(player: Uuid): Team = mod_data.getPlayersTeam(player)
    override fun getActivePlayers() = mod_data.getActivePlayers()
    override fun addActivePlayer(uuid: UUID) = mod_data.addActivePlayer(uuid)
    override fun removeActivePlayer(uuid: UUID) = mod_data.removeActivePlayer(uuid)
    override fun clearActivePlayers() = mod_data.clearActivePlayers()
}