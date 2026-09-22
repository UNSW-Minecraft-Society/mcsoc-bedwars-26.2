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
import kotlin.time.Duration
import mcsoc.bedwars.utils.Team
import mcsoc.bedwars.utils.ticks
import net.minecraft.core.UUIDUtil
import net.minecraft.server.level.ServerLevel
import net.fabricmc.fabric.api.networking.v1.PlayerLookup
import net.minecraft.core.BlockPos
import net.minecraft.util.StringRepresentable
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.saveddata.SavedData
import java.util.UUID
import net.minecraft.world.phys.Vec3
import java.util.Optional
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid
import net.minecraft.world.scores.Scoreboard
import net.minecraft.world.scores.TeamColor
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

enum class GamePhase : StringRepresentable {
    STARTING,
    ACTIVE,
    ENDED,
    INACTIVE;
    
    companion object {
        val CODEC: Codec<GamePhase> = StringRepresentable.fromEnum(GamePhase::values)
    }

    override fun getSerializedName(): String = this.name
}

enum class GamePeriod(val next: GamePeriod?, val startTime: Duration?, val title: String) : StringRepresentable {
    INACTIVE(null, null, "Inactive"),
    TERMINAL(null, 15.minutes, "Game End"),
    DEATHMATCH(TERMINAL, 10.minutes, "Deathmatch"),
    EMERALD_III(DEATHMATCH, 7.minutes, "Emerald Generator III"),
    DIAMOND_III(EMERALD_III, 6.minutes, "Diamond Generator III"),
    EMERALD_II(DIAMOND_III, 4.minutes, "Emerald Generator II"),
    DIAMOND_II(EMERALD_II, 3.minutes, "Emerald Generator II"),
    INITIAL(DIAMOND_II, null, "Game Start");
    
    companion object {
        val CODEC: Codec<GamePeriod> = StringRepresentable.fromEnum(GamePeriod::values)
    }

    override fun getSerializedName(): String = this.name
}

private class PlayerDataRecord() : PlayerInvisHolder, PlayerStateRecord, PlayerTeamState, PlayerUpgradesRecord, PlayerTimeRecord, PlayerStatsRecord {
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

    override var isInvis: Boolean = false
    private var life_state: LifeState = LifeState.ALIVE
    private var team: Team = Team.NONE
    private var toolUpgrades = HashMap<UpgradeItemType, UpgradableItem>()

    private var time_internal: Duration = Duration.ZERO
    override val time get() = RESPAWN_TIME - time_internal
    
    override val timerTick: Int get() = 0
    override var timerSecond: Int = 0
    
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

    override fun tick() {
        if (time < Duration.ZERO) return
        
        val old_time = time
        time_internal += 1.ticks
        timerSecond = (old_time.inWholeSeconds - time.inWholeSeconds).toInt()
    }

    override fun reset() {
        time_internal = Duration.ZERO
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
            UUIDUtil.CODEC.optionalFieldOf("bed_breaker").forGetter{ r -> Optional.ofNullable(r.bedBreaker)},
        ).apply(it, ::TeamDataRecord)}

        private const val PLAYER_RANGE = 15
        private const val TRAP_RANGE = 15
        private const val TRAP_COOLDOWN = 10 * 20
    }

    private var trapCooldown = 0

    override fun tick(level: ServerLevel) {
        if (getUpgrade(TeamUpgradeType.HEAL_POOL)) {
            players
                .mapNotNull {level.getPlayerByUUID(it)}
                .filter { spawn.distanceTo(it.position()) < PLAYER_RANGE }
                .forEach {
                    it.addEffect(MobEffectInstance(MobEffects.REGENERATION, 2 * 20, 0, false, false))
                }
        }

        val haste = getUpgrade(TeamUpgradeType.HASTE)
        if (haste > 0) {
            players
                .mapNotNull {level.getPlayerByUUID(it)}
                .forEach {
                    it.addEffect(MobEffectInstance(MobEffects.HASTE, 2 * 20, haste - 1, false, false))
                }
        }

        if (trapCooldown > 0) {
            trapCooldown--
            return
        }
        
        val playersInBase = PlayerLookup.around(level, bedPosition, TRAP_RANGE.toDouble())
        val enemies = playersInBase.filter { it.uuid !in players }
        val teammates = playersInBase.filter {it.uuid in players}
        if (traps.isNotEmpty() && enemies.isNotEmpty()) {
            val trap = popTrap()
            trap?.enemyEffect(level, enemies)
            trap?.teamEffect(level, teammates)
            
            trapCooldown = TRAP_COOLDOWN
            // todo notify teammates about trap being triggered with title and sfx
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


private class ModDataStore() : PlayerInvisSwitcher, PlayerStateHolder, TeamStateHolder, PlayerUpgradesHolder, PlayerTimeHolder, PlayerStatsHolder, TeamGeneratorHolder, TeamUpgradesHolder,
    LoadedMapHolder {
    companion object {
        val CODEC: Codec<ModDataStore> = RecordCodecBuilder.create{it.group(
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, PlayerDataRecord.CODEC)
                .fieldOf("player_data_map")
                .forGetter(ModDataStore::player_data_map),
            Codec.unboundedMap(Team.CODEC, TeamDataRecord.CODEC)
                .fieldOf("teams_map")
                .forGetter(ModDataStore::teams_map),
            BlockPos.CODEC
                .fieldOf("map_centre")
                .forGetter(ModDataStore::map_centre),
            GamePeriod.CODEC
                .fieldOf("game_period")
                .forGetter(ModDataStore::game_period),
            GamePhase.CODEC
                .fieldOf("game_phase")
                .forGetter(ModDataStore::game_phase)
        ).apply(it, ::ModDataStore)}
    }

    
    private val player_data_map = HashMap<UUID, PlayerDataRecord>()
    private val teams_map = HashMap<Team, TeamDataRecord>()
    private val active_players = mutableSetOf<UUID>()
    private var game_phase = GamePhase.INACTIVE
    private var game_period = GamePeriod.INACTIVE
    override var map_centre: BlockPos = BlockPos(0, 0, 0)
    
    private constructor(
        playerMap: Map<UUID, PlayerDataRecord>,
        teamMap: Map<Team, TeamDataRecord>,
        map_centre: BlockPos,
        game_period: GamePeriod,
        game_phase: GamePhase
    ) : this() {
        this.player_data_map.putAll(playerMap)
        this.teams_map.putAll(teamMap)
        this.map_centre = map_centre
        this.game_phase = game_phase
        this.game_period = game_period
    }


    override fun tick() {
        // Tick down timers for all individual players
        if (game_phase == GamePhase.ACTIVE) {
            for (uuid in active_players) {
                val record = player_data_map[uuid] ?: continue
                record.tick()
            }
        }
    }

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
    
    override fun getPlayerState(player: UUID): PlayerInvisHolder = player_data_map.getOrPut(player) { PlayerDataRecord() }

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
            val scoreboardTeam = scoreboard.addPlayerTeam(it.getName())
            scoreboardTeam.color = Optional.of(it.teamColour)
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


class ModDataTracker : PlayerInvisSwitchExposer, LevelTiedData, PlayerStateExposer, TeamStateExposer, PlayerUpgradesExposer, PlayerTimeExposer, PlayerStatsExposer, TeamGeneratorExposer, TeamUpgradesExposer,
    LoadedMapExposer {
    companion object {
        val CODEC: MapCodec<ModDataTracker> = RecordCodecBuilder.mapCodec{ it.group(
            ModDataStore.CODEC.fieldOf("mod_data").forGetter(ModDataTracker::mod_data)
        ).apply(it, ::ModDataTracker)}
    }
    override fun getType() = LevelDataType.GameState

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
    override fun tickTeams(level: ServerLevel) {
        setDirty()
        mod_data.tickTeams(level)
    }

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
    override fun playerTimerSecondPassed(player: ServerPlayer): Int = mod_data.playerTimerSecondPassed(player)

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
    
    override fun setPlayerInvisibility(player: UUID, invis: Boolean) = mod_data.setPlayerInvisibility(player, invis)
    override fun getPlayerInvisibility(player: UUID): Boolean = mod_data.getPlayerInvisibility(player)
}