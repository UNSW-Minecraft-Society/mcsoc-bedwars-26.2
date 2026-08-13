package mcsoc.bedwars.dataloaders.maploader

import com.sk89q.worldedit.EditSession
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.WorldEditException
import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats
import com.sk89q.worldedit.fabric.FabricAdapter
import com.sk89q.worldedit.function.operation.Operation
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.session.ClipboardHolder
import com.sk89q.worldedit.world.World
import mcsoc.bedwars.BedwarsPlugin
import mcsoc.bedwars.dataloaders.maploader.MapLoader
import net.minecraft.core.BlockPos
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.Level
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.div

const val MAP_DIRECTORY_NAME = "maps"

object SchematicMapLoader : MapLoader {
    override lateinit var maps_directory: Path
    
    lateinit var loaded_map: Clipboard

    override fun init(schem_directory: Path) {
        SchematicMapLoader.maps_directory = schem_directory / MAP_DIRECTORY_NAME
    }
    
    override fun loadMap(mapname: String): Boolean {
        val map_path: Path = maps_directory / mapname
        val format = ClipboardFormats.findByPath(map_path) ?: run {
            BedwarsPlugin.LOGGER.error("Unable to locate map at \"{}\"!", map_path)
            return false
        }
        
        val file = File(map_path.toString())
        try {
            format.getReader(FileInputStream(file)).use{
                loaded_map = it.read()
            }
        } catch (e: IOException) {
            BedwarsPlugin.LOGGER.error("Error while loading map: ", e)
            return false
        }
        
        return true
    }
    
    override fun placeMap(level: Level, pos: BlockPos) {
        val world: World = FabricAdapter.get().fromNativeWorld(level)
        try {
            val editSession: EditSession = WorldEdit.getInstance().newEditSession(world).use{ 
                val operation: Operation = ClipboardHolder(loaded_map)
                    .createPaste(it)
                    .to(FabricAdapter.get().adapt(pos))
                    .copyBiomes(true)
                    // configure here
                .build()
                Operations.complete(operation)
                it
            }
        } catch (e: WorldEditException) {
            BedwarsPlugin.LOGGER.error("Error while placing map: ", e)
        }
    }
}