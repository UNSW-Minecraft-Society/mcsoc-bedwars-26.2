package mcsoc.bedwars.datatrackers.teamstate

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import mcsoc.bedwars.BedwarsPlugin
import mcsoc.bedwars.datatrackers.LevelDataType
import mcsoc.bedwars.datatrackers.LevelTiedData
import mcsoc.bedwars.datatrackers.generatorstate.InvalidTeamException
import mcsoc.bedwars.datatrackers.generatorstate.TeamGeneratorExposer
import mcsoc.bedwars.datatrackers.generatorstate.TeamGeneratorHolder
import mcsoc.bedwars.datatrackers.generatorstate.TeamGeneratorState
import mcsoc.bedwars.datatrackers.playerState
import mcsoc.bedwars.upgrades.TeamUpgrade
import mcsoc.bedwars.upgrades.TeamUpgradeType
import mcsoc.bedwars.upgrades.TrapUpgrade
import mcsoc.bedwars.utils.Team
import net.fabricmc.fabric.api.networking.v1.PlayerLookup
import net.minecraft.core.BlockPos
import net.minecraft.core.UUIDUtil
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.phys.Vec3
import net.minecraft.world.scores.Scoreboard
import java.util.Optional
import java.util.UUID


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

private class TeamDataStore(): TeamStateHolder, TeamGeneratorHolder, TeamUpgradesHolder {
    companion object {
        val CODEC: Codec<TeamDataStore> = RecordCodecBuilder.create{it.group(
            Codec.unboundedMap(Team.CODEC, TeamDataRecord.CODEC)
                .fieldOf("teams_map")
                .forGetter(TeamDataStore::teams_map),
        ).apply(it, ::TeamDataStore)}
    }
    
    private val teams_map = HashMap<Team, TeamDataRecord>()
    
    private constructor(teamMap: Map<Team, TeamDataRecord>) : this() {
        this.teams_map.putAll(teamMap)
    }
    
    fun resetTeamData() = teams_map.clear()
    
    fun tick(level: ServerLevel) {
        for (team in getActiveTeams()) {
            getTeam(team).tick(level)
        }
    }

    override fun getTeam(team: Team): TeamDataRecord {
        return teams_map.getOrPut(team){TeamDataRecord()}
    }

    override fun getActiveTeams(): Iterable<Team> = teams_map.keys.toSet()

    override fun initialiseTeams(teams: Set<Team>, scoreboard: Scoreboard) {
        val new_teams_map: MutableMap<Team, TeamDataRecord> = mutableMapOf()
        teams.forEach{
            new_teams_map[it] = TeamDataRecord()
            
            val scoreboardTeam = scoreboard.addPlayerTeam(it.getName())
            scoreboardTeam.color = Optional.of(it.teamColour)
            scoreboardTeam.isAllowFriendlyFire = false
        }
        new_teams_map.putAll(teams_map.filter{it.key in teams})
        teams_map.clear()
        teams_map.putAll(new_teams_map)
    }

    override fun addPlayer(player: UUID, team: Team, scoreboard: Scoreboard, name: String?) {
        if (team == Team.NONE) return
        
        getTeam(team).addPlayer(player)
        val team = scoreboard.getPlayerTeam(team.getName()) ?: run {
            BedwarsPlugin.LOGGER.error("addPlayer: ", InvalidTeamException(team))
            scoreboard.addPlayerTeam(team.getName())
        }
        if (name != null) scoreboard.addPlayerToTeam(name, team)
    }
}


class TeamDataTracker: LevelTiedData, TeamStateExposer, TeamGeneratorExposer, TeamUpgradesExposer {
    companion object {
        val CODEC: MapCodec<TeamDataTracker> = RecordCodecBuilder.mapCodec{ it.group(
            TeamDataStore.CODEC.fieldOf("team_data").forGetter(TeamDataTracker::team_data)
        ).apply(it, ::TeamDataTracker)}
    }
    override fun getType() = LevelDataType.TeamState

    private val team_data: TeamDataStore
    private constructor(mod_data: TeamDataStore) {
        this.team_data = mod_data
    }
    internal constructor() : this(TeamDataStore())
    fun resetTeamData() = team_data.resetTeamData()
    
    
    fun tick(level: ServerLevel) {
        setDirty()
        team_data.tick(level)
    }

    override fun getBedDestroyed(team: Team): Boolean = team_data.getBedDestroyed(team)
    override fun getBedBreaker(team: Team): UUID? = team_data.getBedBreaker(team)

    override fun getPlayersInTeam(team: Team): List<UUID> = team_data.getPlayersInTeam(team)
    override fun getTeamSpawn(team: Team): Vec3 = team_data.getTeamSpawn(team)
    override fun getActiveTeams(): Iterable<Team> = team_data.getActiveTeams()
    override fun setBedAlive(team: Team, state: Boolean) {
        setDirty()
        team_data.setBedAlive(team, state)
    }

    override fun setBedBreaker(team: Team, player: UUID) {
        setDirty()
        team_data.setBedBreaker(team, player)
    }
    override fun initialiseTeams(teams: Set<Team>, scoreboard: Scoreboard) {
        setDirty()
        team_data.initialiseTeams(teams, scoreboard)
    }
    override fun addPlayer(player: UUID, team: Team, scoreboard: Scoreboard, name: String?) {
        setDirty()
        team_data.addPlayer(player, team, scoreboard, name)
    }
    
    override fun getGenUpgrade(team: Team) = team_data.getGenUpgrade(team)
    override fun upgradeGen(team: Team) {
        setDirty()
        team_data.upgradeGen(team)
    }

    override fun <T> getUpgrade(team: Team, type: TeamUpgradeType<T>) = team_data.getUpgrade(team, type)
    override fun <T> upgrade(team: Team, type: TeamUpgradeType<T>, level: ServerLevel) {
        setDirty()
        team_data.upgrade(team, type, level)
        getPlayersInTeam(team)
            .mapNotNull {level.getPlayerByUUID(it)}
            .forEach { level.playerState.updateItems(it as ServerPlayer) }
    }
    override fun getTraps(team: Team) = team_data.getTraps(team)
    
    override fun getTeamBedPosition(team: Team): BlockPos = team_data.getTeamBedPosition(team)
    override fun setTeamSpawn(team: Team, pos: Vec3) {
        setDirty()
        team_data.setTeamSpawn(team, pos)
    }
    override fun setTeamBedPosition(team: Team, pos: BlockPos) {
        setDirty()
        team_data.setTeamBedPosition(team, pos)
    }

    override fun popTrap(team: Team): TrapUpgrade? {
        setDirty()
        return team_data.popTrap(team)
    }
    override fun addTrap(team: Team, type: TrapUpgrade) {
        setDirty()
        team_data.addTrap(team, type)
    }
}