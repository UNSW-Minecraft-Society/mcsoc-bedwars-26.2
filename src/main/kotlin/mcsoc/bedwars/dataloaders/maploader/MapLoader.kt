package mcsoc.bedwars.dataloaders.maploader

import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import java.nio.file.Path


interface MapLoader {
    companion object {
        fun getMapLoader(): MapLoader {
            return SchematicMapLoader
        }
    }
    
    var maps_directory: Path
    fun init(schem_directory: Path)
    
    fun loadMap(mapname: String): Boolean
    fun placeMap(level: Level, pos: BlockPos)
}