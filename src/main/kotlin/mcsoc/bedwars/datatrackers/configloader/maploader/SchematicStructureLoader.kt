package mcsoc.bedwars.dataloaders.maploader

import com.sk89q.worldedit.EditSession
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.WorldEditException
import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats
import com.sk89q.worldedit.fabric.FabricAdapter
import com.sk89q.worldedit.function.operation.Operation
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.session.ClipboardHolder
import com.sk89q.worldedit.world.World
import mcsoc.bedwars.BedwarsPlugin
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.Level
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.div

const val MAP_DIRECTORY_NAME = "maps"

data class LoadedSchematic(
    val pos: BlockPos,
    val schematic: Clipboard, 
)

class SchematicStructureLoader(
    level_key: ResourceKey<Level>,
) : StructureLoader(level_key) {
    
    val schematic_queue: ArrayDeque<LoadedSchematic> = ArrayDeque()
    
    override fun loadStructure(structure_name: String, pos: BlockPos): Boolean {
        val map_path: Path = structures_directory / "$structure_name.schem"
        val format = ClipboardFormats.findByPath(map_path) ?: run {
            BedwarsPlugin.LOGGER.error("Unable to locate map at \"{}\"!", map_path)
            return false
        }
        
        val file = File(map_path.toString())
        return try {
            format.getReader(FileInputStream(file)).use{
                schematic_queue.add(LoadedSchematic(pos, it.read()))
                true
            }
        } catch (e: IOException) {
            BedwarsPlugin.LOGGER.error("Error while loading map: ", e)
            false
        }
    }
    
    
    override fun placeQueuedStructures(server: MinecraftServer) {
        if (schematic_queue.isEmpty()) return
        
        val world: World = FabricAdapter.get().fromNativeWorld(server.getLevel(level_key))
        while (schematic_queue.isNotEmpty()) {
            WorldEdit.getInstance().newEditSession(world).use{session ->
                val schematic = schematic_queue.removeFirstOrNull() ?: return
                try {
                    val operation: Operation = ClipboardHolder(schematic.schematic)
                        .createPaste(session)
                        .to(FabricAdapter.get().adapt(schematic.pos))
                        .copyBiomes(true)
                        // configure here
                    .build()
                    Operations.complete(operation)
                } catch (e: WorldEditException) {
                    BedwarsPlugin.LOGGER.error("Error while placing map: ", e)
                }
            }
        }
    }
}