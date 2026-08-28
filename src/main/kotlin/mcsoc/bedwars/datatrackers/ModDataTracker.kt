package mcsoc.bedwars.datatrackers

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import kotlinx.serialization.Serializable
import mcsoc.bedwars.generators.DefaultGeneratorTypes
import mcsoc.bedwars.utils.inWholeTicks
import mcsoc.bedwars.utils.ticks
import kotlin.time.Duration
import kotlin.time.TimeSource
import mcsoc.bedwars.utils.Team
import net.minecraft.core.UUIDUtil
import net.minecraft.server.level.ServerPlayer
import mcsoc.bedwars.generators.Generator
import mcsoc.bedwars.generators.GeneratorFactory
import mcsoc.bedwars.generators.IslandGenerator
import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.saveddata.SavedData
import java.util.UUID
import net.minecraft.world.phys.Vec3


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
    private val players: MutableList<UUID> = mutableListOf(),
    private var bedAlive: Boolean = true,
    private val spawn: Vec3 = Vec3(0.0, 0.0, 0.0),
    private val generators: MutableList<Generator> = mutableListOf()
) : TeamStateRecord {
    companion object {
        val CODEC: Codec<TeamDataRecord> = RecordCodecBuilder.create { it.group(
            UUIDUtil.CODEC.listOf().fieldOf("players").forGetter(TeamDataRecord::players),
            Codec.BOOL.fieldOf("bed_alive").forGetter(TeamDataRecord::bedAlive),
            Vec3.CODEC.fieldOf("spawn").forGetter(TeamDataRecord::spawn),
            Generator.CODEC.listOf().fieldOf("generators").forGetter(TeamDataRecord::generators)
        ).apply(it, ::TeamDataRecord)}
    }

    override fun getBedAlive(): Boolean = bedAlive
    override fun getSpawn(): Vec3 = spawn
    override fun getPlayers(): MutableList<UUID> = players
    override fun getGenerators(): List<Generator> = generators

    override fun setBedAlive(bedAlive: Boolean) {
       this.bedAlive = bedAlive
    }

    override fun addPlayer(player: UUID) {
        players.add(player)
    }

    override fun addGenerator(gen: Generator) {
        generators.add(gen)    
    }

    override fun upgradeGenerators() {
        generators.filterIsInstance<IslandGenerator>().forEach(IslandGenerator::upgrade)
    }
}


private class ModDataStore() : SavedData(), PlayerStateHolder, TeamStateHolder, Ticker, GeneratorsHolder {
    companion object {
        val CODEC: Codec<ModDataStore> = RecordCodecBuilder.create{it.group(
            Codec.unboundedMap(UUIDUtil.CODEC, PlayerDataRecord.CODEC)
                .fieldOf("player_data_map")
                .forGetter(ModDataStore::player_data_map),

            Codec.unboundedMap(Team.CODEC, TeamDataRecord.CODEC)
                .fieldOf("teams_map")
                .forGetter(ModDataStore::teams_map),

            Codec.STRING.xmap(Duration::parseIsoString, Duration::toIsoString)
                .fieldOf("game_timer")
                .forGetter(ModDataStore::game_timer),

            Codec.list(Generator.CODEC)
                .fieldOf("generators")
                .forGetter(ModDataStore::generators)
        ).apply(it, ::ModDataStore)}
    }    
    
    private val player_data_map = HashMap<UUID, PlayerDataRecord>()
    private val teams_map = HashMap<Team, TeamDataRecord>()
    private val generators = ArrayList<Generator>()
    private val active_players = mutableSetOf<UUID>()
    private var prev_tick_time = TimeSource.Monotonic.markNow()
    private var tick_delta = Duration.ZERO
    private var game_timer = Duration.ZERO
    private var timer_tick = false
    
    
    private constructor(
        playerMap: Map<UUID, PlayerDataRecord>,
        teamMap: Map<Team, TeamDataRecord>,
        timer: Duration,
        gens: List<Generator>
    ) : this() {
        this.player_data_map.putAll(playerMap)
        this.teams_map.putAll(teamMap)
        this.game_timer = timer
        this.generators.addAll(gens)
    }
    
    
    override fun tick(server: MinecraftServer) {
        tick_delta = prev_tick_time.elapsedNow()
        prev_tick_time = TimeSource.Monotonic.markNow()
        
        timer_tick = game_timer.inWholeTicks != (game_timer + tick_delta).inWholeTicks
        game_timer += tick_delta
        
        getGenerators().forEach { it.tick(server) }
    }

    override fun getGameTime() = game_timer
        
    override fun getTimerTick() = timer_tick

    private fun getPlayerData(id: UUID): PlayerDataRecord {
        return player_data_map.getOrPut(id) { PlayerDataRecord() }
    }

    private fun getPlayerData(player: Player): PlayerDataRecord {
        return getPlayerData(player.uuid)
    }

    override fun getPlayerState(player: Player): PlayerDataRecord {
        return getPlayerData(player)
    }

    override fun addGenerator(gen: Generator) {
        generators.add(gen)
    }
    
    override fun addGenerator(type: String, location: Vec3, dim: ResourceKey<Level>, team: Team): Boolean {
        val config = DefaultGeneratorTypes.generators[type] ?: return false
        val gen = GeneratorFactory.createGenerator(config, location, dim)
        addGenerator(gen)
        getTeam(team).addGenerator(gen)
        return true
    }
    
    override fun getGenerators(): List<Generator> = generators

    override fun removeGenerator(gen: Generator) {
        generators.remove(gen)
    }
    
    override fun upgradeTeamGenerators(team: Team) {
        getTeam(team).upgradeGenerators()
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

    override fun addPlayer(player: UUID, team: Team) {
        getTeam(team).addPlayer(player)
        getPlayerData(player).setTeamName(team)
    }
    
    override fun getPlayersTeam(player: UUID): Team = getPlayerData(player).getTeamName()
    
    override fun addActivePlayer(uuid: UUID) = active_players.add(uuid)
    override fun removeActivePlayer(uuid: UUID) = active_players.remove(uuid)
    override fun getActivePlayers() = active_players
}


object ModDataTracker : PlayerStateExposer, TeamStateExposer, TickExposer, GeneratorsExposer {
    private val mod_data = ModDataStore()

    override fun tick(server: MinecraftServer) = mod_data.tick(server)
    override fun getGameTime(): Duration = mod_data.getGameTime()
    override fun getTimerTick(): Boolean = mod_data.getTimerTick()

    override fun isPlayerAlive(player: Player) = mod_data.isPlayerAlive(player)
    override fun isPlayerRespawning(player: Player) = mod_data.isPlayerRespawning(player)
    override fun isPlayerDead(player: Player) = mod_data.isPlayerDead(player)

    override fun getBedDestroyed(team: Team): Boolean = mod_data.getBedDestroyed(team)
    override fun getPlayersInTeam(team: Team): List<UUID> = mod_data.getPlayersInTeam(team)
    override fun getTeamSpawn(team: Team): Vec3 = mod_data.getTeamSpawn(team)
    override fun getActiveTeams(): List<Team> = mod_data.getActiveTeams()
    override fun setBedAlive(team: Team, state: Boolean) = mod_data.setBedAlive(team, state)
    override fun initialiseTeams(numTeams: Int) = mod_data.initialiseTeams(numTeams)
    override fun addPlayer(player: UUID, team: Team) = mod_data.addPlayer(player, team)
    
    override fun getPlayersTeam(player: UUID): Team = mod_data.getPlayersTeam(player)
    override fun getActivePlayers() = mod_data.getActivePlayers()
    override fun addActivePlayer(uuid: UUID) = mod_data.addActivePlayer(uuid)
    override fun removeActivePlayer(uuid: UUID) = mod_data.removeActivePlayer(uuid)

    override fun addGenerator(type: String, location: Vec3, dim: ResourceKey<Level>) = mod_data.addGenerator(type, location, dim)
    override fun addGenerator(type: String, location: Vec3, dim: ResourceKey<Level>, team: Team) = mod_data.addGenerator(type, location, dim, team)
    override fun removeGenerator(location: Vec3) = mod_data.removeGenerator(location)
    override fun upgradeGeneratorTier() = mod_data.upgradeGeneratorTier()
    override fun upgradeTeamGenerators(team: Team) = mod_data.upgradeTeamGenerators(team)
}