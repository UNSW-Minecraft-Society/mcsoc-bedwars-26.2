package mcsoc.bedwars.datatrackers.configloader.maploader

import com.sk89q.worldedit.EditSession
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.WorldEditException
import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats
import com.sk89q.worldedit.fabric.FabricAdapter
import com.sk89q.worldedit.function.operation.Operation
import com.sk89q.worldedit.function.operation.RunContext
import com.sk89q.worldedit.math.transform.AffineTransform
import com.sk89q.worldedit.session.ClipboardHolder
import com.sk89q.worldedit.world.World
import mcsoc.bedwars.BedwarsPlugin
import mcsoc.bedwars.datatrackers.configloader.BedwarsConfigData
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.Level
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.math.PI
import kotlin.math.round


const val MAP_DIRECTORY_NAME = "maps"
val OPERATIONS_PER_TICK by lazy { BedwarsConfigData.paste_ops_per_second }

data class LoadedSchematic(
    val pos: BlockPos,
    val rot: Double,
    val schematic: Clipboard, 
)

data class OperationInProgress(
    var inProgress: Operation? = null,
    var inProgressSession: EditSession? = null
) {
    fun isEmpty(): Boolean = (inProgress == null)
    
    fun useLoadedSchematic(world: World, schematic: LoadedSchematic) {
        inProgressSession = WorldEdit.getInstance().newEditSession(world)
        inProgress = ClipboardHolder(schematic.schematic)
            .also{ it.transform = AffineTransform().rotateY(round(schematic.rot * 180 / PI)) }
            .createPaste(inProgressSession)
            .to(FabricAdapter.get().adapt(schematic.pos))
            .copyBiomes(true)
            .copyEntities(true)
            // configure here
        .build()
    }
    
    fun step(): Boolean {
        try {
            inProgress = inProgress?.resume(RunContext())
        } catch (e: WorldEditException) {
            BedwarsPlugin.LOGGER.error("Error while placing map: ", e)
        }
        if (inProgress == null) {
            inProgressSession?.close()
            inProgressSession = null
        }
        return isEmpty()
    }
}

class SchematicStructureLoader(
    level_key: ResourceKey<Level>,
) : StructureLoader(level_key) {
    
    val schematic_queue: ArrayDeque<LoadedSchematic> = ArrayDeque()
    var inProgress: OperationInProgress = OperationInProgress()
    
    override fun loadStructure(structure_name: String, pos: BlockPos, rot: Double): Boolean {
        val map_path: Path = structures_directory / "$structure_name.schem"
        val format = ClipboardFormats.findByPath(map_path) ?: run {
            BedwarsPlugin.LOGGER.error("Unable to locate map at \"{}\"!", map_path)
            return false
        }
        
        val file = File(map_path.toString())
        return try {
            format.getReader(FileInputStream(file)).use{
                schematic_queue.add(LoadedSchematic(pos, rot, it.read()))
                true
            }
        } catch (e: IOException) {
            BedwarsPlugin.LOGGER.error("Error while loading map: ", e)
            false
        }
    }
    
    
    override fun placeQueuedStructures(server: MinecraftServer) {
        if (inProgress.isEmpty()) {
            if (schematic_queue.isEmpty()) return
            
            val schematic = schematic_queue.removeFirstOrNull() ?: return
            val world: World = FabricAdapter.get().fromNativeWorld(server.getLevel(level_key))
            inProgress.useLoadedSchematic(world, schematic)
        }
        for (i in 0u until OPERATIONS_PER_TICK) {
            if (!inProgress.step()) break
        }
    }
}