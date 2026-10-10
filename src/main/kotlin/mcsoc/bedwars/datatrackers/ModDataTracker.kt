package mcsoc.bedwars.datatrackers

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import mcsoc.bedwars.BedwarsPlugin
import mcsoc.bedwars.datatrackers.configloader.BedwarsConfigData
import mcsoc.bedwars.datatrackers.generatorstate.TeamGeneratorExposer
import mcsoc.bedwars.datatrackers.generatorstate.TeamGeneratorHolder
import mcsoc.bedwars.datatrackers.generatorstate.TeamGeneratorState
import mcsoc.bedwars.datatrackers.generatorstate.InvalidTeamException
import mcsoc.bedwars.datatrackers.mapdata.LoadedMapExposer
import mcsoc.bedwars.datatrackers.mapdata.LoadedMapHolder
import net.minecraft.server.level.ServerPlayer
import mcsoc.bedwars.upgrades.TeamUpgrade
import mcsoc.bedwars.upgrades.TeamUpgradeType
import mcsoc.bedwars.upgrades.TrapUpgrade
import kotlin.time.Duration
import mcsoc.bedwars.utils.Team
import net.minecraft.core.UUIDUtil
import net.minecraft.server.level.ServerLevel
import net.fabricmc.fabric.api.networking.v1.PlayerLookup
import net.minecraft.core.BlockPos
import net.minecraft.util.StringRepresentable
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import java.util.UUID
import net.minecraft.world.phys.Vec3
import java.util.Optional
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid
import net.minecraft.world.scores.Scoreboard


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

enum class GamePeriod(val next: GamePeriod?, val title: String, val notif: String) : StringRepresentable {
    INACTIVE(null, "Inactive", ""),
    TERMINAL(null, "Game End", "Game has ended."),
    DEATHMATCH(TERMINAL, "Deathmatch", "Deathmatch has begun."),
    EMERALD_III(DEATHMATCH, "Emerald Generator III", "Emerald generators have been upgraded."),
    DIAMOND_III(EMERALD_III, "Diamond Generator III", "Diamond generators have been upgraded."),
    EMERALD_II(DIAMOND_III, "Emerald Generator II", "Emerald generators have been upgraded."),
    DIAMOND_II(EMERALD_II, "Diamond Generator II", "Diamond generators have been upgraded."),
    INITIAL(DIAMOND_II, "Game Start", "Game has begun.");
    
    val startTime: Duration? 
        get() = BedwarsConfigData.generator_times[this]
    
    companion object {
        val CODEC: Codec<GamePeriod> = StringRepresentable.fromEnum(GamePeriod::values)
    }

    override fun getSerializedName(): String = this.name
}


private class ModDataStore():  LoadedMapHolder {
    companion object {
        val CODEC: Codec<ModDataStore> = RecordCodecBuilder.create{it.group(
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

    private val active_players = mutableSetOf<UUID>()
    private var game_phase = GamePhase.INACTIVE
    private var game_period = GamePeriod.INACTIVE
    override var map_centre: BlockPos = BlockPos(0, 0, 0)
    
    private constructor(
        map_centre: BlockPos,
        game_period: GamePeriod,
        game_phase: GamePhase
    ) : this() {
        this.map_centre = map_centre
        this.game_phase = game_phase
        this.game_period = game_period
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
    
    fun resetModData() {
        active_players.clear()
        game_phase = GamePhase.INACTIVE
        game_period = GamePeriod.INACTIVE
        map_centre = BlockPos(0, 0, 0)
    }

    fun addActivePlayer(uuid: UUID) = active_players.add(uuid)
    fun removeActivePlayer(uuid: UUID) = active_players.remove(uuid)
    fun getActivePlayers() = active_players
    fun clearActivePlayers() = active_players.clear()
}

// better active players
fun ServerLevel.getActivePlayers(): Iterable<ServerPlayer> = this.gameState.getActivePlayers().mapNotNull(this.server.playerList::getPlayer)


class ModDataTracker: LevelTiedData,  LoadedMapExposer {
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
    fun resetModData() = mod_data.resetModData()
    
    override var map_centre 
        get() = mod_data.map_centre
        set(v) { mod_data.map_centre = v }

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

    fun getActivePlayers() = mod_data.getActivePlayers()
    fun addActivePlayer(uuid: UUID): Boolean {
        setDirty()
        return mod_data.addActivePlayer(uuid)
    }
    fun removeActivePlayer(uuid: UUID): Boolean {
        setDirty()
        return mod_data.removeActivePlayer(uuid)
    }
    fun clearActivePlayers() {
        setDirty()
        mod_data.clearActivePlayers()
    }    
}