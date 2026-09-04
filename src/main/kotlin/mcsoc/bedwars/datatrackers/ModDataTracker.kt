package mcsoc.bedwars.datatrackers

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import mcsoc.bedwars.datatrackers.generatorData.TeamGeneratorExposer
import mcsoc.bedwars.datatrackers.generatorData.TeamGeneratorHolder
import mcsoc.bedwars.datatrackers.generatorData.TeamGeneratorState
import mcsoc.bedwars.utils.inWholeTicks
import kotlin.time.Duration
import kotlin.time.TimeSource
import mcsoc.bedwars.utils.Team
import net.minecraft.core.UUIDUtil
import mcsoc.bedwars.generators.BaseGenerator
import mcsoc.bedwars.upgrades.UpgradableItem
import mcsoc.bedwars.upgrades.UpgradeItemType
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.saveddata.SavedData
import java.util.UUID
import net.minecraft.world.phys.Vec3

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

private class PlayerDataRecord() : PlayerStateRecord, PlayerTeamState, PlayerUpgradesRecord {
    companion object {
        val TOOL_UPGRADES_CODEC: Codec<HashMap<UpgradeItemType, UpgradableItem>> =
            Codec.unboundedMap(UpgradeItemType.CODEC, Codec.STRING).xmap(
                    { HashMap(it.mapValues { (type, tier) -> type.fromName(tier) }) },
                    { it.mapValues { (_, item) -> (item as Enum<*>).name } }
                )
    
        val CODEC: Codec<PlayerDataRecord> = RecordCodecBuilder.create{it.group(
            LifeState.CODEC.fieldOf("life_state").forGetter(PlayerDataRecord::life_state),
            Team.CODEC.fieldOf("team").forGetter(PlayerDataRecord::team),
            TOOL_UPGRADES_CODEC
                .fieldOf("player_upgrades")
                .forGetter(PlayerDataRecord::toolUpgrades)
        ).apply(it, ::PlayerDataRecord)}
    }

    private var life_state: LifeState = LifeState.ALIVE
    private var team: Team = Team.NONE

    private var toolUpgrades = HashMap<UpgradeItemType, UpgradableItem>()

    private constructor(
        life_state: LifeState,
        team: Team,
        toolUpgrades: Map<UpgradeItemType, UpgradableItem>
    ) : this() {
        this.life_state = life_state
        this.team = Team.NONE
        this.toolUpgrades.putAll(toolUpgrades)
    }

    override fun getLifeState(): LifeState {
        return this.life_state
    }

    override fun getTeamName(): Team = team

    override fun setTeamName(team: Team) {
        this.team = team
    }

    override fun getItem(item: UpgradeItemType): UpgradableItem {
        return toolUpgrades.getOrPut(item) { item.default }
    }

    override fun setItem(item: UpgradableItem) {
        toolUpgrades[item.type] = item
    }

    override fun removeItem(item: UpgradeItemType) {
        toolUpgrades.remove(item)
    }
}


private class TeamDataRecord(
    private val players: MutableList<UUID> = mutableListOf(),
    private var bedAlive: Boolean = true,
    private val spawn: Vec3 = Vec3(0.0, 0.0, 0.0),
) : TeamStateRecord, TeamGeneratorState {
    companion object {
        val CODEC: Codec<TeamDataRecord> = RecordCodecBuilder.create { it.group(
            UUIDUtil.CODEC.listOf().fieldOf("players").forGetter(TeamDataRecord::players),
            Codec.BOOL.fieldOf("bed_alive").forGetter(TeamDataRecord::bedAlive),
            Vec3.CODEC.fieldOf("spawn").forGetter(TeamDataRecord::spawn),
        ).apply(it, ::TeamDataRecord)}
    }
    
    private lateinit var generator: BaseGenerator
    
    override fun getBedAlive(): Boolean = bedAlive
    override fun getSpawn(): Vec3 = spawn
    override fun getPlayers(): MutableList<UUID> = players
    override fun getGenerator() = generator

    override fun setBedAlive(bedAlive: Boolean) {
       this.bedAlive = bedAlive
    }

    override fun addPlayer(player: UUID) {
        players.add(player)
    }

    override fun setGenerator(gen: BaseGenerator) {
        generator = gen    
    }

    override fun upgradeGenerator() {
        if (::generator.isInitialized) generator.upgrade()
    }
}


private class ModDataStore() : SavedData(), PlayerStateHolder, TeamStateHolder, Ticker, PlayerUpgradesHolder, TeamGeneratorHolder {
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
        ).apply(it, ::ModDataStore)}
    }    
    
    private val player_data_map = HashMap<UUID, PlayerDataRecord>()
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
        playerMap: Map<UUID, PlayerDataRecord>,
        teamMap: Map<Team, TeamDataRecord>,
        timer: Duration,
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
        
        getGenerators().forEach { it.tick() }
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

    private fun getPlayerData(id: UUID): PlayerDataRecord {
        return player_data_map.getOrPut(id) { PlayerDataRecord() }
    }

    private fun getPlayerData(player: Player): PlayerDataRecord {
        return getPlayerData(player.uuid)
    }

    override fun getPlayerState(player: Player): PlayerDataRecord {
        return getPlayerData(player)
    }

    override fun getItemUpgradeState(player: Player): PlayerUpgradesRecord {
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

    override fun addPlayer(player: UUID, team: Team) {
        getTeam(team).addPlayer(player)
        getPlayerData(player).setTeamName(team)
    }
    
    override fun getPlayersTeam(player: UUID): Team = getPlayerData(player).getTeamName()
    
    override fun addActivePlayer(uuid: UUID) = active_players.add(uuid)
    override fun removeActivePlayer(uuid: UUID) = active_players.remove(uuid) // there could be other things to do when removing player
    override fun getActivePlayers() = active_players
    override fun clearActivePlayers() = active_players.clear()
}


class ModDataTracker : PlayerStateExposer, TeamStateExposer, TickExposer, PlayerUpgradesExposer, TeamGeneratorExposer {
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
    override fun clearActivePlayers() = mod_data.clearActivePlayers()

    override fun upgradeItem(player: ServerPlayer, item: UpgradeItemType) = mod_data.upgradeItem(player, item)
    override fun downgradeItems(player: ServerPlayer) = mod_data.downgradeItems(player)
    override fun clearItems(player: ServerPlayer) = mod_data.clearItems(player)

    override fun getNextItemStack(player: ServerPlayer, item: UpgradeItemType) = mod_data.getNextItemStack(player, item)
    override fun getTier(player: ServerPlayer, item: UpgradeItemType) = mod_data.getTier(player, item)

    override fun getGenerator(team: Team) = mod_data.getGenerator(team)
    override fun setGenerator(team: Team, gen: BaseGenerator) = mod_data.setGenerator(team, gen)
    override fun upgradeGenerator(team: Team) = mod_data.upgradeGenerator(team)
}