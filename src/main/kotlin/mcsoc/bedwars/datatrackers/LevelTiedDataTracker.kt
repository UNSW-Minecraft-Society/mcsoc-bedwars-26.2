@file:JvmName("LevelData")
package mcsoc.bedwars.datatrackers

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import mcsoc.bedwars.BedwarsPlugin
import mcsoc.bedwars.datatrackers.blockprotection.BlockProtectionTracker
import mcsoc.bedwars.datatrackers.generatorstate.GeneratorDataTracker
import mcsoc.bedwars.datatrackers.mapdata.MapDataTracker
import mcsoc.bedwars.datatrackers.playerstate.PlayerDataTracker
import mcsoc.bedwars.datatrackers.teamstate.TeamDataTracker
import mcsoc.bedwars.datatrackers.timetracker.GameTimeTracker
import mcsoc.bedwars.datatrackers.timetracker.GameTimer
import mcsoc.bedwars.datatrackers.timetracker.TickExposer
import mcsoc.bedwars.gui.ShopConfig
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.datafix.DataFixTypes
import net.minecraft.world.level.saveddata.SavedData
import net.minecraft.world.level.saveddata.SavedDataType


// each tracker should extend this class
abstract class LevelTiedData {
    companion object {                
        internal val CODEC: Codec<LevelTiedData> = LevelDataType.CODEC.dispatch(
            { inst -> inst.getType() },
            { type -> type.codec }
        )
    }
    internal var isDirty: Boolean = false
    protected fun setDirty() {
        this.isDirty = true
    }
    
    abstract fun getType(): LevelDataType<*>
}

private class LevelTiedDataTracker() : SavedData() {
    var clockSpeed: Double = 1.0
    var shopConfig: ShopConfig = ShopConfig.NORMAL
    private val tracked_data: MutableMap<LevelDataType<*>, LevelTiedData> = mutableMapOf()
    constructor(map: Map<LevelDataType<*>, LevelTiedData>) : this() {
        tracked_data.putAll(map)
    }
    
    companion object {
        private val CODEC: Codec<LevelTiedDataTracker> = RecordCodecBuilder.create{it.group(
            Codec.unboundedMap(LevelDataType.CODEC, LevelTiedData.CODEC)
                .fieldOf("level_data")
                .forGetter(LevelTiedDataTracker::tracked_data)
        ).apply(it, ::LevelTiedDataTracker)}
        
        private val TYPE = SavedDataType<LevelTiedDataTracker>(
            BedwarsPlugin.id("saved_level_data"),
            ::LevelTiedDataTracker, CODEC, DataFixTypes.LEVEL
        )
        fun getLevelData(level: ServerLevel): LevelTiedDataTracker = level.dataStorage.computeIfAbsent(TYPE)
    }
    override fun isDirty(): Boolean = tracked_data.values.any(LevelTiedData::isDirty)
    override fun setDirty(dirty: Boolean) {
        super.setDirty(dirty)
        for (tracker in tracked_data.values) {
            tracker.isDirty = dirty
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <T : LevelTiedData> getDataOfType(type: LevelDataType<T>): T {
        return this.tracked_data.getOrPut(type){type.default} as T 
    }
}

private val ServerLevel.levelTiedData get() = LevelTiedDataTracker.getLevelData(this)


// codec can be null, indicating that the datatype should be level-tied but not saved
sealed class LevelDataType<T : LevelTiedData>(val id: String, codec: MapCodec<T>?, val default: T) {
    companion object {
        private val REGISTRY: Map<String, LevelDataType<*>> by lazy {
            LevelDataType::class.sealedSubclasses
                .mapNotNull { it.objectInstance }
                .associateBy { it.id }
        }
        fun fromId(id: String): LevelDataType<*> = REGISTRY[id] ?: throw IllegalArgumentException("Unknown LevelDataType: \"$id\"")
            
        val CODEC: Codec<LevelDataType<*>> = Codec.STRING.xmap(::fromId, LevelDataType<*>::id)
    }
    val codec: MapCodec<T> = codec ?: MapCodec.unit(default)
    
    // put another enum value for each tracked data type
    object TimeState : LevelDataType<GameTimeTracker>("time_state", GameTimeTracker.CODEC, GameTimeTracker())
    object PlayerState : LevelDataType<PlayerDataTracker>("player_state", PlayerDataTracker.CODEC, PlayerDataTracker())
    object TeamState : LevelDataType<TeamDataTracker>("player_state", TeamDataTracker.CODEC, TeamDataTracker())
    object BlockProtection: LevelDataType<BlockProtectionTracker>("block_protection", BlockProtectionTracker.CODEC, BlockProtectionTracker())
    object GeneratorState : LevelDataType<GeneratorDataTracker>("generator_state", GeneratorDataTracker.CODEC, GeneratorDataTracker())
    object CustomEntityData : LevelDataType<CustomEntityDataTracker>("custom_entity_data", CustomEntityDataTracker.CODEC, CustomEntityDataTracker())
    object EventQueue : LevelDataType<GameEventTracker>("event_queue", GameEventTracker.CODEC, GameEventTracker())
    object GameClock : LevelDataType<GameTimer>("timer", GameTimer.CODEC, GameTimer())
    object ActivePlayers : LevelDataType<ActivePlayersTracker>("active_players", ActivePlayersTracker.CODEC, ActivePlayersTracker())
    object MapDataState : LevelDataType<MapDataTracker>("map_data_state", MapDataTracker.CODEC, MapDataTracker())
}


val ServerLevel.timeState: GameTimeTracker get() = levelTiedData.getDataOfType(LevelDataType.TimeState)
val ServerPlayer.timeState: GameTimeTracker
    get() = level().timeState
val ServerLevel.playerState: PlayerDataTracker get() = levelTiedData.getDataOfType(LevelDataType.PlayerState)
val ServerPlayer.playerState: PlayerDataTracker
    get() = level().playerState
val ServerLevel.teamState: TeamDataTracker get() = levelTiedData.getDataOfType(LevelDataType.TeamState)
val ServerPlayer.teamState: TeamDataTracker
    get() = level().teamState
val ServerLevel.blockProtection: BlockProtectionTracker get() = levelTiedData.getDataOfType(LevelDataType.BlockProtection)
val ServerLevel.generatorState: GeneratorDataTracker get() = levelTiedData.getDataOfType(LevelDataType.GeneratorState)
val ServerLevel.activePlayers: ActivePlayersTracker get() = levelTiedData.getDataOfType(LevelDataType.ActivePlayers)
val ServerLevel.mapData: MapDataTracker get() = levelTiedData.getDataOfType(LevelDataType.MapDataState)
val ServerLevel.customEntityData get() = levelTiedData.getDataOfType(LevelDataType.CustomEntityData)
val ServerLevel.eventQueue get() = levelTiedData.getDataOfType(LevelDataType.EventQueue).also{it.level = this}
val ServerLevel.clock: TickExposer get() = levelTiedData.getDataOfType(LevelDataType.GameClock).also{it.level = this}
var ServerLevel.clockSpeed: Double
    get() = levelTiedData.clockSpeed
    set(v) {levelTiedData.clockSpeed = v}
var ServerLevel.shopConfig: ShopConfig
    get() = levelTiedData.shopConfig
    set(v) {levelTiedData.shopConfig = v}
// put other level-tied data getters here