package mcsoc.bedwars.datatrackers.mapdata

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import mcsoc.bedwars.datatrackers.LevelDataType
import mcsoc.bedwars.datatrackers.LevelTiedData
import net.minecraft.core.BlockPos

private class MapDataStore(): LoadedMapHolder {
    companion object {
        val CODEC: Codec<MapDataStore> = RecordCodecBuilder.create{it.group(
            BlockPos.CODEC
                .fieldOf("map_centre")
                .forGetter(MapDataStore::map_centre),
        ).apply(it, ::MapDataStore)}
    }
    
    override var map_centre: BlockPos = BlockPos(0, 0, 0)
    
    private constructor(map_centre: BlockPos) : this() {
        this.map_centre = map_centre
    }
    
    fun reset() {
        map_centre = BlockPos(0, 0, 0)
    }
}

class MapDataTracker: LevelTiedData, LoadedMapExposer {
     companion object {
        val CODEC: MapCodec<MapDataTracker> = RecordCodecBuilder.mapCodec{ it.group(
            MapDataStore.CODEC.fieldOf("mod_data").forGetter(MapDataTracker::mod_data)
        ).apply(it, ::MapDataTracker)}
    }
    override fun getType() = LevelDataType.MapDataState

    private val mod_data: MapDataStore
    private constructor(mod_data: MapDataStore) {
        this.mod_data = mod_data
    }
    internal constructor() : this(MapDataStore())
    fun resetMapData() = mod_data.reset()
    
    override var map_centre 
        get() = mod_data.map_centre
        set(v) { mod_data.map_centre = v }

}
