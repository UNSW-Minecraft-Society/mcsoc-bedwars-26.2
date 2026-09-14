package mcsoc.bedwars.datatrackers

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import mcsoc.bedwars.BedwarsPlugin
import mcsoc.bedwars.datatrackers.generatorstate.TeamGeneratorExposer
import mcsoc.bedwars.datatrackers.generatorstate.TeamGeneratorHolder
import mcsoc.bedwars.datatrackers.generatorstate.TeamGeneratorState
import mcsoc.bedwars.datatrackers.generatorstate.InvalidTeamException
import mcsoc.bedwars.datatrackers.mapdata.LoadedMapExposer
import mcsoc.bedwars.datatrackers.mapdata.LoadedMapHolder
import mcsoc.bedwars.gamestate.RESPAWN_TIME
import mcsoc.bedwars.upgrades.UpgradableItem
import mcsoc.bedwars.upgrades.UpgradeItemType
import net.minecraft.server.level.ServerPlayer
import mcsoc.bedwars.upgrades.TeamUpgrade
import mcsoc.bedwars.upgrades.TeamUpgradeType
import mcsoc.bedwars.upgrades.TrapUpgrade
import mcsoc.bedwars.utils.inWholeTicks
import kotlin.time.Duration
import kotlin.time.TimeSource
import mcsoc.bedwars.utils.Team
import net.minecraft.core.UUIDUtil
import net.minecraft.server.level.ServerLevel
import net.fabricmc.fabric.api.networking.v1.PlayerLookup
import net.minecraft.core.BlockPos
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.saveddata.SavedData
import java.util.UUID
import net.minecraft.world.phys.Vec3
import java.util.Optional
import kotlin.math.ceil
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid
import net.minecraft.world.scores.Scoreboard

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

private class PlayerDataRecord() : PlayerStateRecord, PlayerTeamState, PlayerUpgradesRecord, PlayerTimeRecord, PlayerStatsRecord {
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

    private var respawn_ticks: Int = 0
    private var respawn_seconds: Int = 0
    private var respawn_second_passed: Boolean = false

    private var kills: Int = 0
    private var final_kills: Int = 0
    private var deaths: Int = 0
    private var beds_destroyed: Int = 0

    private constructor(
        life_state: LifeState,
        team: Team,
        toolUpgrades: Map<UpgradeItemType, UpgradableItem>
    ) : this() {
        this.life_state = life_state
        this.team = team
        this.toolUpgrades.putAll(toolUpgrades)
    }

    override fun getLifeState(): LifeState {
        return this.life_state
    }

    override fun setLifeState(new_state: LifeState) {
        this.life_state = new_state
    }

    override fun getDeathPosition(): Vec3 {
        if (this.life_state is LifeState.DEAD) return (this.life_state as LifeState.DEAD).death_position
        return Vec3.ZERO
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

    override fun getRespawnSeconds(): Int {
        return respawn_seconds
    }

    override fun decrementPlayerRespawnTicks() {
        if (respawn_ticks > 0) {
            respawn_ticks -= 1
            if (ceil((respawn_ticks / 20.0)) < respawn_seconds) {
                respawn_seconds -= 1
                respawn_second_passed = true
            } else respawn_second_passed = false
        }
    }

    override fun resetPlayerRespawnTime() {
        respawn_ticks = RESPAWN_TIME * 20
        respawn_seconds = RESPAWN_TIME
    }

    override fun getSecondPassed(): Boolean {
        return respawn_second_passed
    }

    override fun getKills(): Int {
        return kills
    }

    override fun getFinalKills(): Int {
        return final_kills
    }

    override fun getDeaths(): Int {
        return deaths
    }

    override fun getBedsDestroyed(): Int {
        return beds_destroyed
    }

    override fun setKills(value: Int) {
        kills = value
    }

    override fun setFinalKills(value: Int) {
        final_kills = value
    }

    override fun setDeaths(value: Int) {
        deaths = value
    }

    override fun setBedsDestroyed(value: Int) {
        beds_destroyed = value
    }
}

private class TeamDataRecord(
    private val players: MutableList<UUID> = mutableListOf(),
    private var bedAlive: Boolean = true,
    private var bedPosition: BlockPos = BlockPos(0, 0, 0),
    private var genUpgrade: Int = 0,
    private var spawn: Vec3 = Vec3(0.0, 0.0, 0.0),
    bedBreakerOptional: Optional<UUID> = Optional.empty()
) : TeamStateRecord, TeamGeneratorState, TeamUpgradesState {
    private var bedBreaker: UUID?
    init { bedBreaker = bedBreakerOptional.orElse(null) }
    
    companion object {
        val UUID_LIST_CODEC: Codec<MutableList<Uuid>> = UUIDUtil.CODEC.listOf().xmap(
            { it.map(UUID::toKotlinUuid).toMutableList() },
            { it.map(Uuid::toJavaUuid) }
        )

        val CODEC: Codec<TeamDataRecord> = RecordCodecBuilder.create { it.group(
            UUIDUtil.CODEC.listOf().fieldOf("players").forGetter(TeamDataRecord::players),
            Codec.BOOL.fieldOf("bed_alive").forGetter(TeamDataRecord::bedAlive),
            BlockPos.CODEC.fieldOf("bed_position").forGetter(TeamDataRecord::bedPosition),
            Codec.INT.fieldOf("gen_upgrade").forGetter(TeamDataRecord::genUpgrade),
            Vec3.CODEC.fieldOf("spawn").forGetter(TeamDataRecord::spawn),
            UUIDUtil.CODEC.optionalFieldOf("bed_breaker").forGetter{Optional.ofNullable(it.bedBreaker)},
        ).apply(it, ::TeamDataRecord)}

        private const val PLAYER_RANGE = 15
        private const val TRAP_COOLDOWN = 10 * 20
    }

    private var trapCooldown = 0

    fun tick(level: ServerLevel) {

        if (getUpgrade(TeamUpgradeType.HEAL_POOL)) {
            players
                .mapNotNull {level.getPlayerByUUID(it)}
                .filter { spawn.distanceTo(it.position()) < PLAYER_RANGE }
                .forEach { it.addEffect(MobEffectInstance(MobEffects.REGENERATION, 1, 0, false, false)) }
        }

        val haste = getUpgrade(TeamUpgradeType.HASTE)
        if (haste > 0) {
            players
                .mapNotNull {level.getPlayerByUUID(it)}
                .forEach { it.addEffect(MobEffectInstance(MobEffects.HASTE, 1, haste - 1, false, false)) }
        }

        if (trapCooldown > 0) {
            trapCooldown--
            return
        }

        val playersInBase = PlayerLookup.around(level, spawn, PLAYER_RANGE.toDouble())
        val enemies = playersInBase.filter { it.uuid !in players }
        val teammates = playersInBase.filter {it.uuid in players}
        if (traps.isNotEmpty() && enemies.isNotEmpty()) {
            val trap = popTrap()
            trap?.enemyEffect(level, enemies)
            trap?.teamEffect(level, teammates)
            // notify teammates about trap being triggered with title and sfx
        }
    }

    override fun getBedAlive(): Boolean = bedAlive
    override fun getBedBreaker(): UUID? = bedBreaker

    override fun getSpawn(): Vec3 = spawn
    override fun getPlayers(): MutableList<UUID> = players

    override fun setBedAlive(bedAlive: Boolean) {
        this.bedAlive = bedAlive
    }

    override fun setBedBreaker(bedBreaker: UUID) {
        this.bedBreaker = bedBreaker
    }

    override fun addPlayer(player: UUID) {
        players.add(player)
    }

    override fun getGenUpgrade(): Int = genUpgrade
    override fun upgradeGen() {
        genUpgrade++
    }

    private val upgrades = mutableMapOf<TeamUpgradeType<*>, TeamUpgrade<*>>()

    @Suppress("UNCHECKED_CAST")
    override fun <T> getUpgrade(type: TeamUpgradeType<T>): T {
        val upgrade = upgrades.getOrPut(type) { type.default() }
        return (upgrade as TeamUpgrade<T>).value
    }

    override fun <T> upgrade(type: TeamUpgradeType<T>) {
        val upgrade = upgrades.getOrPut(type) { type.default() }
        upgrade.upgrade()
    }

    private var traps = mutableListOf<TrapUpgrade>()

    override fun getTraps(): List<TrapUpgrade> = traps
    override fun popTrap(): TrapUpgrade? = traps.removeFirstOrNull()

    override fun addTrap(type: TrapUpgrade) {
        traps.add(type)
    }
    
    override fun getBedPosition(): BlockPos = bedPosition
    override fun setSpawn(pos: Vec3) {
        spawn = pos
    }
    override fun setBedPosition(pos: BlockPos) {
        bedPosition = pos
    }
}


private class ModDataStore() : SavedData(), PlayerStateHolder, TeamStateHolder, Ticker, PlayerUpgradesHolder, PlayerTimeHolder, PlayerStatsHolder, TeamGeneratorHolder, TeamUpgradesHolder,
    LoadedMapHolder {
    companion object {
        val CODEC: Codec<ModDataStore> = RecordCodecBuilder.create{it.group(
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, PlayerDataRecord.CODEC)
                .fieldOf("player_data_map")
                .forGetter(ModDataStore::player_data_map),

            Codec.unboundedMap(Team.CODEC, TeamDataRecord.CODEC)
                .fieldOf("teams_map")
                .forGetter(ModDataStore::teams_map),

            Codec.STRING.xmap(Duration::parseIsoString, Duration::toIsoString)
                .fieldOf("game_timer")
                .forGetter(ModDataStore::game_timer),
                    
            BlockPos.CODEC
                .fieldOf("map_centre")
                .forGetter(ModDataStore::map_centre),
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
    override var map_centre: BlockPos = BlockPos(0, 0, 0)
    
    private constructor(
        playerMap: Map<UUID, PlayerDataRecord>,
        teamMap: Map<Team, TeamDataRecord>,
        timer: Duration,
        map_centre: BlockPos
    ) : this() {
        this.player_data_map.putAll(playerMap)
        this.teams_map.putAll(teamMap)
        this.game_timer = timer
        this.map_centre = map_centre
    }


    override fun tick() {
        tick_delta = prev_tick_time.elapsedNow()
        prev_tick_time = TimeSource.Monotonic.markNow()

        timer_tick = game_timer.inWholeTicks != (game_timer + tick_delta).inWholeTicks
        timer_second = game_timer.inWholeSeconds != (game_timer + tick_delta).inWholeSeconds

        // Tick down timers for all individual players
        if (game_phase == GamePhase.ACTIVE) {
            active_players.forEach { uuid ->
                val record = player_data_map.getOrDefault(uuid, null)
                if (record != null && timer_tick) record.decrementPlayerRespawnTicks()
            }
        }

        game_timer += tick_delta
    }

    fun tickTeams(level: ServerLevel) {
        getActiveTeams().forEach { teams_map[it]?.tick(level) }
    }

    override fun getGameTime() = game_timer

    override fun resetGameTime() {
        game_timer = Duration.ZERO
        prev_tick_time = TimeSource.Monotonic.markNow()
    }

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

    fun resetModData() {
        player_data_map.clear()
        teams_map.clear()
        active_players.clear()
        prev_tick_time = TimeSource.Monotonic.markNow()
        tick_delta = Duration.ZERO
        game_timer = Duration.ZERO
        timer_tick = false
        timer_second = false
        game_phase = GamePhase.INACTIVE
        game_period = GamePeriod.INACTIVE
        map_centre = BlockPos(0, 0, 0)
    }

    override fun getPlayerState(player: Player): PlayerDataRecord {
        return getPlayerData(player)
    }

    override fun getItemUpgradeState(player: Player): PlayerUpgradesRecord {
        return getPlayerData(player)
    }

    override fun getPlayerTime(player: Player): PlayerDataRecord {
        return getPlayerData(player)
    }

    override fun getPlayerStats(uuid: UUID): PlayerStatsRecord {
        return getPlayerData(uuid)
    }

    override fun getTeam(team: Team): TeamDataRecord {
        return teams_map[team] ?: run {
            BedwarsPlugin.LOGGER.error("getTeam: ", InvalidTeamException(team))
            TeamDataRecord()
        }
    }

    override fun getActiveTeams(): List<Team> = teams_map.keys.toList()

    override fun initialiseTeams(teams: Set<Team>, scoreboard: Scoreboard) {
        teams_map.clear()
        
        for (team in scoreboard.playerTeams) scoreboard.removePlayerTeam(team)
        teams.forEach { 
            teams_map[it] = TeamDataRecord()
            scoreboard.addPlayerTeam(it.getName())
        }
        
        for (scoreboardTeam in scoreboard.playerTeams) scoreboardTeam.isAllowFriendlyFire = false
    }

    override fun addPlayer(player: UUID, team: Team, scoreboard: Scoreboard, name: String?) {
        getTeam(team).addPlayer(player)
        getPlayerData(player).setTeamName(team)
        val team = scoreboard.getPlayerTeam(team.getName()) ?: run {
            BedwarsPlugin.LOGGER.error("addPlayer: ", InvalidTeamException(team))
            scoreboard.addPlayerTeam(team.getName())
        }
        if (name != null) scoreboard.addPlayerToTeam(name, team)
    }

    override fun getPlayersTeam(player: UUID): Team = getPlayerData(player).getTeamName()

    override fun addActivePlayer(uuid: UUID) = active_players.add(uuid)
    override fun removeActivePlayer(uuid: UUID) = active_players.remove(uuid) // there could be other things to do when removing player
    override fun getActivePlayers() = active_players
    override fun clearActivePlayers() = active_players.clear()
}


class ModDataTracker : LevelTiedData, PlayerStateExposer, TeamStateExposer, TickExposer, PlayerUpgradesExposer, PlayerTimeExposer, PlayerStatsExposer, TeamGeneratorExposer, TeamUpgradesExposer,
    LoadedMapExposer {
    companion object {
        val CODEC: MapCodec<ModDataTracker> = RecordCodecBuilder.mapCodec{ it.group(
            ModDataStore.CODEC.fieldOf("mod_data").forGetter(ModDataTracker::mod_data)
        ).apply(it, ::ModDataTracker)}
    }
    override val type get() = LevelDataType.GameState

    private val mod_data: ModDataStore
    private constructor(mod_data: ModDataStore) {
        this.mod_data = mod_data
    }
    internal constructor() : this(ModDataStore())
    
    override var map_centre 
        get() = mod_data.map_centre
        set(v) { mod_data.map_centre = v }

    override fun tick() {
        setDirty()
        mod_data.tick()
    }
    fun tickTeams(level: ServerLevel) {
        setDirty()
        mod_data.tickTeams(level)
    }
    override fun getGameTime(): Duration = mod_data.getGameTime()
    override fun resetGameTime() {
        setDirty()
        mod_data.resetGameTime()
    }
    override fun getTimerTick(): Boolean = mod_data.getTimerTick()
    override fun getTimerSecond(): Boolean = mod_data.getTimerSecond()

    fun getGamePhase(): GamePhase = mod_data.getGamePhase()
    fun setGamePhase(phase: GamePhase) {
        setDirty()
        mod_data.setGamePhase(phase)
    }
    fun getGamePeriod(): GamePeriod = mod_data.getGamePeriod()
    fun setGamePeriod(period: GamePeriod) {
        setDirty()
        mod_data.setGamePeriod(period)
    }

    fun resetModData() = mod_data.resetModData()

    override fun isPlayerAlive(player: Player) = mod_data.isPlayerAlive(player)
    override fun isPlayerRespawning(player: Player) = mod_data.isPlayerRespawning(player)
    override fun getPlayerDeathPosition(player: Player): Vec3 = mod_data.getPlayerDeathPosition(player)
    override fun isPlayerEliminated(player: Player): Boolean = mod_data.isPlayerEliminated(player)

    override fun setPlayerAlive(player: Player) = mod_data.setPlayerAlive(player)
    override fun setPlayerRespawning(player: Player) = mod_data.setPlayerRespawning(player)
    override fun setPlayerDead(player: Player, position: Vec3) = mod_data.setPlayerDead(player, position)
    override fun setPlayerEliminated(player: Player) = mod_data.setPlayerEliminated(player)

    override fun getBedDestroyed(team: Team): Boolean = mod_data.getBedDestroyed(team)
    override fun getBedBreaker(team: Team): UUID? = mod_data.getBedBreaker(team)

    override fun getPlayersInTeam(team: Team): List<UUID> = mod_data.getPlayersInTeam(team)
    override fun getTeamSpawn(team: Team): Vec3 = mod_data.getTeamSpawn(team)
    override fun getActiveTeams(): List<Team> = mod_data.getActiveTeams()
    override fun setBedAlive(team: Team, state: Boolean) {
        setDirty()
        mod_data.setBedAlive(team, state)
    }

    override fun setBedBreaker(team: Team, player: UUID) {
        setDirty()
        mod_data.setBedBreaker(team, player)
    }
    override fun initialiseTeams(teams: Set<Team>, scoreboard: Scoreboard) {
        setDirty()
        mod_data.initialiseTeams(teams, scoreboard)
    }
    override fun addPlayer(player: UUID, team: Team, scoreboard: Scoreboard, name: String?) {
        setDirty()
        mod_data.addPlayer(player, team, scoreboard, name)
    }
    
    override fun getPlayersTeam(player: UUID): Team = mod_data.getPlayersTeam(player)
    override fun getActivePlayers() = mod_data.getActivePlayers()
    override fun addActivePlayer(uuid: UUID): Boolean {
        setDirty()
        return mod_data.addActivePlayer(uuid)
    }
    override fun removeActivePlayer(uuid: UUID): Boolean {
        setDirty()
        return mod_data.removeActivePlayer(uuid)
    }
    override fun clearActivePlayers() {
        setDirty()
        mod_data.clearActivePlayers()
    }

    override fun upgradeItem(player: ServerPlayer, item: UpgradeItemType) {
        setDirty()
        mod_data.upgradeItem(player, item)
    }
    override fun downgradeItems(player: ServerPlayer) {
        setDirty()
        mod_data.downgradeItems(player)
    }

    override fun updateItems(player: ServerPlayer) {
        // Don't think this needs to setDirty(), updating the enchant should already set it dirty, either way none of the data actually changes
        mod_data.updateItems(player)
    }
    override fun clearItems(player: ServerPlayer) {
        setDirty()
        mod_data.clearItems(player)
    }
    override fun getNextItemStack(player: ServerPlayer, item: UpgradeItemType) = mod_data.getNextItemStack(player, item)
    override fun getTier(player: ServerPlayer, item: UpgradeItemType) = mod_data.getTier(player, item)

    override fun getGenUpgrade(team: Team) = mod_data.getGenUpgrade(team)
    override fun upgradeGen(team: Team) {
        setDirty()
        mod_data.upgradeGen(team)
    }

    override fun <T> getUpgrade(team: Team, type: TeamUpgradeType<T>) = mod_data.getUpgrade(team, type)
    override fun <T> upgrade(team: Team, type: TeamUpgradeType<T>, level: ServerLevel) {
        setDirty()
        mod_data.upgrade(team, type, level)
        if (level == null) return
        for (playerId in getPlayersInTeam(team)) {
            val player = level.getPlayerByUUID(playerId)
            if (player is ServerPlayer) updateItems(player)
        }
    }
    override fun getTraps(team: Team) = mod_data.getTraps(team)
    
    override fun getTeamBedPosition(team: Team): BlockPos = mod_data.getTeamBedPosition(team)
    override fun setTeamSpawn(team: Team, pos: Vec3) {
        setDirty()
        mod_data.setTeamSpawn(team, pos)
    }
    override fun setTeamBedPosition(team: Team, pos: BlockPos) {
        setDirty()
        mod_data.setTeamBedPosition(team, pos)
    }

    override fun getPlayerRespawnSeconds(player: ServerPlayer): Int = mod_data.getPlayerRespawnSeconds(player)
    override fun resetPlayerRespawnTime(player: ServerPlayer) {
        setDirty()
        mod_data.resetPlayerRespawnTime(player)
    }
    override fun playerTimerSecondPassed(player: ServerPlayer): Boolean = mod_data.playerTimerSecondPassed(player)

    override fun getPlayerKills(uuid: UUID): Int = mod_data.getPlayerKills(uuid)
    override fun getPlayerFinalKills(uuid: UUID): Int = mod_data.getPlayerFinalKills(uuid)
    override fun getPlayerDeaths(uuid: UUID): Int = mod_data.getPlayerDeaths(uuid)
    override fun getPlayerBedsDestroyed(uuid: UUID): Int = mod_data.getPlayerBedsDestroyed(uuid)

    override fun setPlayerKills(uuid: UUID, value: Int) = mod_data.setPlayerKills(uuid, value)
    override fun setPlayerFinalKills(uuid: UUID, value: Int) = mod_data.setPlayerFinalKills(uuid, value)
    override fun setPlayerDeaths(uuid: UUID, value: Int) = mod_data.setPlayerDeaths(uuid, value)
    override fun setPlayerBedsDestroyed(uuid: UUID, value: Int) = mod_data.setPlayerBedsDestroyed(uuid, value)
    override fun popTrap(team: Team): TrapUpgrade? {
        setDirty()
        return mod_data.popTrap(team)
    }
    override fun addTrap(team: Team, type: TrapUpgrade) {
        setDirty()
        mod_data.addTrap(team, type)
    }
}