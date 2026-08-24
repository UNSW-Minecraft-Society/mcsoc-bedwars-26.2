package mcsoc.bedwars.datatrackers.configloader.maploader

import mcsoc.bedwars.BedwarsPlugin
import mcsoc.bedwars.datatrackers.configloader.config_dir
import java.nio.file.Path

import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer
import java.util.concurrent.CompletableFuture
import kotlin.io.path.createFile
import kotlin.io.path.createParentDirectories
import kotlin.io.path.div
import kotlin.io.path.notExists


const val STRUCTURES_DIRECTORY = "maps"

val structures_directory: Path 
    get() = config_dir / STRUCTURES_DIRECTORY
abstract class StructureLoader {
    companion object {
        fun getNewLoader(level_key: ResourceKey<Level>): StructureLoader {
            return SchematicStructureLoader(level_key)
        }
        
        val loaders_map: MutableMap<ResourceKey<Level>, StructureLoader> = mutableMapOf()
        
        private fun Level.getStructureLoader(): StructureLoader {
            return loaders_map.getOrPut(this.dimension()){getNewLoader(this.dimension())}
        }
        
        fun Level.place(structure: String, pos: BlockPos): CompletableFuture<Boolean> {
            return this.getStructureLoader().queueStructure(structure, pos)
        }
        
        fun initialise() {
            structures_directory.createParentDirectories().takeIf{it.notExists()}?.createFile()
        }
    }
    
    val level_key: ResourceKey<Level>

    internal constructor(level_key: ResourceKey<Level>) {
        this.level_key = level_key
        if (StructureLoader.loaders_map[level_key] != null) throw IndexOutOfBoundsException("Cannot register multiple schematic loaders per world!")
        StructureLoader.loaders_map[level_key] = this
        ServerTickEvents.END_SERVER_TICK.register{
            placeQueuedStructures(it)
        }
    }
    
    internal abstract fun loadStructure(structure_name: String, pos: BlockPos): Boolean
    fun queueStructure(structure_name: String, pos: BlockPos): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            loadStructure(structure_name, pos)
        }
    }
    
    internal abstract fun placeQueuedStructures(server: MinecraftServer)
}