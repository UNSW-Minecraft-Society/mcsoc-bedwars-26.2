package mcsoc.bedwars

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.world.level.saveddata.SavedData

class ModDataStore(val player_data_tracker: PlayerDataTracker = PlayerDataTracker()) : SavedData() {
    companion object {
        val CODEC: Codec<ModDataStore> = RecordCodecBuilder.create{it.group(
                PlayerDataTracker.CODEC.fieldOf("player_data_tracker").forGetter(ModDataStore::getPlayerDataTracker)
            ).apply(it, ::ModDataStore)
        }
    }    
    
    fun getPlayerDataTracker(): PlayerDataTracker {
        return player_data_tracker
    }
}
