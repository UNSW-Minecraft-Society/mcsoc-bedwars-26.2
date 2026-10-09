package mcsoc.bedwars.datatrackers.playerstate

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import mcsoc.bedwars.datatrackers.GamePhase
import mcsoc.bedwars.datatrackers.LevelDataType
import mcsoc.bedwars.datatrackers.LevelTiedData
import mcsoc.bedwars.datatrackers.PlayerTeamState
import mcsoc.bedwars.gamestate.RESPAWN_TIME
import mcsoc.bedwars.upgrades.UpgradableItem
import mcsoc.bedwars.upgrades.UpgradeItemType
import mcsoc.bedwars.utils.Team
import mcsoc.bedwars.utils.ticks
import net.minecraft.core.UUIDUtil
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import java.util.UUID
import kotlin.time.Duration

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

private class PlayerDataStore(): PlayerInvisSwitcher, PlayerStateHolder, PlayerUpgradesHolder, PlayerTimeHolder, PlayerStatsHolder {
    companion object {
        val CODEC: Codec<PlayerDataStore> = RecordCodecBuilder.create{it.group(
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, PlayerDataRecord.CODEC)
                .fieldOf("player_data_map")
                .forGetter(PlayerDataStore::player_data_map),
        ).apply(it, ::PlayerDataStore)}
    }
    
    private val player_data_map = HashMap<UUID, PlayerDataRecord>()
    
    private constructor(
        playerMap: Map<UUID, PlayerDataRecord>,
    ) : this() {
        this.player_data_map.putAll(playerMap)
    }
    
    
    override fun tick() {
        player_data_map.values.forEach(PlayerDataRecord::tick)
    }
    
    private fun getPlayerData(id: UUID): PlayerDataRecord {
        return player_data_map.getOrPut(id) { PlayerDataRecord() }
    }

    private fun getPlayerData(player: Player): PlayerDataRecord {
        return getPlayerData(player.uuid)
    }
    
    override fun getPlayerState(player: UUID): PlayerInvisHolder = getPlayerData(player)
    override fun getPlayerState(player: Player): PlayerStateRecord = getPlayerData(player)
    override fun getItemUpgradeState(player: Player): PlayerUpgradesRecord = getPlayerData(player)
    override fun getPlayerStats(uuid: UUID): PlayerStatsRecord  = getPlayerData(uuid)
    override fun getPlayerTime(player: Player): PlayerTimeRecord = getPlayerData(player)

    fun resetPlayerData() {
        player_data_map.clear()
    }
    
    fun setTeam(player: UUID, team: Team) = getPlayerData(player).setTeamName(team)
    fun getPlayersTeam(player: UUID): Team = getPlayerData(player).getTeamName()
    
}

class PlayerDataTracker: LevelTiedData, PlayerStateExposer, PlayerUpgradesExposer, PlayerTimeExposer, PlayerStatsExposer, PlayerInvisSwitchExposer {
    companion object {
        val CODEC: MapCodec<PlayerDataTracker> = RecordCodecBuilder.mapCodec{ it.group(
            PlayerDataStore.CODEC.fieldOf("mod_data").forGetter(PlayerDataTracker::player_data)
        ).apply(it, ::PlayerDataTracker)}
    }
    override fun getType() = LevelDataType.PlayerState

    private val player_data: PlayerDataStore
    private constructor(mod_data: PlayerDataStore) {
        this.player_data = mod_data
    }
    internal constructor() : this(PlayerDataStore())
    
    override fun tick() {
        setDirty()
        player_data.tick()
    }
    
    
    fun resetPlayerData() = player_data.resetPlayerData()

    override fun isPlayerAlive(player: Player) = player_data.isPlayerAlive(player)
    override fun isPlayerRespawning(player: Player) = player_data.isPlayerRespawning(player)
    override fun getPlayerDeathPosition(player: Player): Vec3 = player_data.getPlayerDeathPosition(player)
    override fun isPlayerEliminated(player: Player): Boolean = player_data.isPlayerEliminated(player)

    override fun setPlayerAlive(player: Player) = player_data.setPlayerAlive(player)
    override fun setPlayerRespawning(player: Player) = player_data.setPlayerRespawning(player)
    override fun setPlayerDead(player: Player, position: Vec3) = player_data.setPlayerDead(player, position)
    override fun setPlayerEliminated(player: Player) = player_data.setPlayerEliminated(player)

    override fun upgradeItem(player: ServerPlayer, item: UpgradeItemType) {
        setDirty()
        player_data.upgradeItem(player, item)
    }
    override fun downgradeItems(player: ServerPlayer) {
        setDirty()
        player_data.downgradeItems(player)
    }
    override fun updateItems(player: ServerPlayer) = player_data.updateItems(player)
    override fun clearItems(player: ServerPlayer) {
        setDirty()
        player_data.clearItems(player)
    }
    override fun getNextItemStack(player: ServerPlayer, item: UpgradeItemType) = player_data.getNextItemStack(player, item)
    override fun getTier(player: ServerPlayer, item: UpgradeItemType) = player_data.getTier(player, item)

    override fun getPlayerRespawnSeconds(player: ServerPlayer): Int = player_data.getPlayerRespawnSeconds(player)
    override fun resetPlayerRespawnTime(player: ServerPlayer) {
        setDirty()
        player_data.resetPlayerRespawnTime(player)
    }
    override fun playerTimerSecondPassed(player: ServerPlayer): Int = player_data.playerTimerSecondPassed(player)

    override fun getPlayerKills(uuid: UUID): Int = player_data.getPlayerKills(uuid)
    override fun getPlayerFinalKills(uuid: UUID): Int = player_data.getPlayerFinalKills(uuid)
    override fun getPlayerDeaths(uuid: UUID): Int = player_data.getPlayerDeaths(uuid)
    override fun getPlayerBedsDestroyed(uuid: UUID): Int = player_data.getPlayerBedsDestroyed(uuid)

    override fun setPlayerKills(uuid: UUID, value: Int) = player_data.setPlayerKills(uuid, value)
    override fun setPlayerFinalKills(uuid: UUID, value: Int) = player_data.setPlayerFinalKills(uuid, value)
    override fun setPlayerDeaths(uuid: UUID, value: Int) = player_data.setPlayerDeaths(uuid, value)
    override fun setPlayerBedsDestroyed(uuid: UUID, value: Int) = player_data.setPlayerBedsDestroyed(uuid, value)
    
    
    override fun setPlayerInvisibility(player: UUID, invis: Boolean) = player_data.setPlayerInvisibility(player, invis)
    override fun getPlayerInvisibility(player: UUID): Boolean = player_data.getPlayerInvisibility(player)
    
    fun setTeam(player: UUID, team: Team) = player_data.setTeam(player, team)
    fun getPlayersTeam(player: UUID): Team = player_data.getPlayersTeam(player)
}
