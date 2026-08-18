package mcsoc.bedwars.dataloaders.maploader

import mcsoc.bedwars.dataloaders.maploader.StructureLoader.Companion.getStructureLoader
import mcsoc.bedwars.utils.CylindricalBlockPos
import net.minecraft.world.level.Level


internal class BedwarsIsland (
    val position: CylindricalBlockPos,
    val structure_name: String
) {
    fun place(level: Level) {
        level.getStructureLoader().queueStructure(structure_name, position.toBlockPos())
    }
}


internal class BedwarsMapStructure(
    val level: Level,
    val base_islands: List<BedwarsIsland> = mutableListOf(),
    val diamond_islands: List<BedwarsIsland> = mutableListOf(),
    val mid_island: BedwarsIsland,
    val misc_islands: List<BedwarsIsland> = mutableListOf()
) {
    fun place() {
        // also register generators
        base_islands.forEach{it.place(level)}
        diamond_islands.forEach{it.place(level)}
        misc_islands.forEach{it.place(level)}
        mid_island.place(level)
    }
}
