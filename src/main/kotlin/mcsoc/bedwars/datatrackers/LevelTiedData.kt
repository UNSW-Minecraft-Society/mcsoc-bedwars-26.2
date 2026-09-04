package mcsoc.bedwars.datatrackers

import mcsoc.bedwars.datatrackers.generatorData.GeneratorDataTracker
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level


private data class LevelTiedData(
    val game_state: ModDataTracker = ModDataTracker(),
    val generator_state: GeneratorDataTracker = GeneratorDataTracker()
    // put other level-tied data trackers here
)

private val level_data_map: MutableMap<ResourceKey<Level>, LevelTiedData> = mutableMapOf()
val ServerLevel.gameState get() = level_data_map.getOrPut(this.dimension()){LevelTiedData()}.game_state
val ServerLevel.generators get() = level_data_map.getOrPut(this.dimension()){LevelTiedData()}.generator_state

// put other level-tied data getters here