package mcsoc.bedwars.datatrackers

import com.mojang.serialization.codecs.RecordCodecBuilder
import mcsoc.bedwars.BedwarsPlugin
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.datafix.DataFixTypes
import net.minecraft.world.level.saveddata.SavedData
import net.minecraft.world.level.saveddata.SavedDataType


private data class LevelTiedData(
    val game_state: ModDataTracker = ModDataTracker()
    // put other level-tied data trackers here
) : SavedData() {
    companion object {
        private val CODEC = RecordCodecBuilder.create{it.group(
            ModDataTracker.CODEC.fieldOf("game_state").forGetter(LevelTiedData::game_state)
            // put codecs here
        ).apply(it, ::LevelTiedData)}
        
        private val TYPE = SavedDataType<LevelTiedData>(
            Identifier.fromNamespaceAndPath(BedwarsPlugin.MOD_ID, "saved_level_data"),
            ::LevelTiedData, CODEC, DataFixTypes.LEVEL
        )
        
        fun getLevelData(level: ServerLevel): LevelTiedData {
            val level_data = level.dataStorage.computeIfAbsent(TYPE)
            // just do this to make sure, for now
            level_data.isDirty = true
            return level_data
        } 
    }
}

// REMEMBER TO USE THIS
fun ServerLevel.setDirty() {
    TODO()
}

val ServerLevel.gameState: ModDataTracker get() = LevelTiedData.getLevelData(this).game_state
// put other level-tied data getters here