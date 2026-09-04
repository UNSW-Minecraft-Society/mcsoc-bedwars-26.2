package mcsoc.bedwars.datatrackers

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import mcsoc.bedwars.BedwarsPlugin
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.datafix.DataFixTypes
import net.minecraft.world.level.saveddata.SavedData
import net.minecraft.world.level.saveddata.SavedDataType
import java.util.EnumMap
import java.util.EnumSet
import java.util.Optional
import kotlin.reflect.KClass
import kotlin.reflect.cast
import kotlin.reflect.safeCast


// each tracker should extend this class
abstract class LevelTiedData {
    companion object {
        internal val CODEC: Codec<LevelTiedData> = LevelDataType.CODEC.dispatch(
            { inst -> inst.type },
            { type -> type.codec }
        )
    }
    internal var isDirty: Boolean = false
    protected fun setDirty() {
        this.isDirty = true
    }
    
    abstract val type: LevelDataType<*>
}

sealed class LevelDataType<T : LevelTiedData>(val id: String, val codec: MapCodec<T>, val default: T) {
    companion object {
        fun fromId(id: String): LevelDataType<*> {
            return when (id) {
                else -> GameState
            }
        }
        val CODEC: Codec<LevelDataType<*>> = Codec.STRING.xmap(::fromId, LevelDataType<*>::id)
    }
    object GameState : LevelDataType<ModDataTracker>("game_state", ModDataTracker.CODEC, ModDataTracker())
    // put another enum value for each tracked data type
}

private class LevelTiedDataTracker() : SavedData() {
    private val tracked_data: MutableMap<LevelDataType<*>, LevelTiedData> = mutableMapOf(
        Pair(LevelDataType.GameState, ModDataTracker()),
        // register default entries for each enum value 
    )
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
            Identifier.fromNamespaceAndPath(BedwarsPlugin.MOD_ID, "saved_level_data"),
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
    
    fun getDataOfType(type: LevelDataType<*>): LevelTiedData {
        return this.tracked_data.getOrPut(type){type.default}
    }
}

private val ServerLevel.levelTiedData get() = LevelTiedDataTracker.getLevelData(this)

val ServerLevel.gameState: ModDataTracker get() = levelTiedData.getDataOfType(LevelDataType.GameState) as ModDataTracker
// put other level-tied data getters here